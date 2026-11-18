#!/usr/bin/env python3
"""Local-only FaceNet threshold evaluator for FaceRecon.

The app and this script share the same matching rule:
  * every embedding is L2-normalized (cosine ranking via Euclidean distance),
  * each person is represented by the mean of their closest three samples,
  * a match is accepted only below `threshold`,
  * a close second place (gap < margin) is reported as UNSURE.

This tool never uploads anything. Put your own images in eval/images/, using
one folder per student. No student photos are shipped with the repository.

Example:
    python3 tools/eval_recognition.py \
        --model app/src/main/assets/facenet.tflite \
        --images eval/images \
        --output eval/report.csv
"""

from __future__ import annotations

import argparse
import csv
import math
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    sys.exit("Pillow is required: pip install pillow numpy")

try:
    import numpy as np
except ImportError:
    sys.exit("numpy is required: pip install pillow numpy")

try:
    from tflite_runtime.interpreter import Interpreter
except ImportError:
    try:
        from tensorflow.lite.python.interpreter import Interpreter
    except ImportError:
        sys.exit("Install a TFLite runtime: pip install tflite-runtime")


IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}
INPUT_SIZE = 160


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--model",
        type=Path,
        default=Path("app/src/main/assets/facenet.tflite"),
        help="TFLite FaceNet model (default: 128-d facenet.tflite)",
    )
    parser.add_argument(
        "--images",
        type=Path,
        default=Path("eval/images"),
        help="Dataset root: eval/images/<Student Name>/*.jpg",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("eval/report.csv"),
        help="Where to write the per-image report",
    )
    parser.add_argument("--dim", type=int, default=128, help="Embedding size (128 or 512)")
    parser.add_argument("--threshold", type=float, default=1.0, help="Acceptance threshold")
    parser.add_argument("--margin", type=float, default=0.1, help="Ambiguity margin")
    parser.add_argument("--samples-per-person", type=int, default=3, help="Top-k samples per person")
    return parser.parse_args()


def l2_normalize(vector: np.ndarray) -> np.ndarray:
    norm = float(np.linalg.norm(vector))
    if not math.isfinite(norm) or norm == 0.0:
        return np.zeros_like(vector)
    return (vector / norm).astype(np.float32)


def preprocess(path: Path) -> np.ndarray:
    """Bilinear resize to 160x160 then per-image standardization (matches app)."""
    with Image.open(path) as image:
        image = image.convert("RGB").resize((INPUT_SIZE, INPUT_SIZE), Image.Resampling.BILINEAR)
        pixels = np.asarray(image, dtype=np.float32)

    mean = float(pixels.mean())
    std = float(pixels.std())
    std = max(std, 1.0 / math.sqrt(pixels.size))
    return ((pixels - mean) / std).astype(np.float32)


def load_embeddings(model: Path, images_dir: Path, expected_dim: int):
    if not model.is_file():
        sys.exit(f"Model not found: {model}")
    if not images_dir.is_dir():
        sys.exit(f"Dataset folder not found: {images_dir}. Create eval/images/<Name>/*.jpg first.")

    interpreter = Interpreter(model_path=str(model))
    interpreter.allocate_tensors()
    input_details = interpreter.get_input_details()[0]
    output_details = interpreter.get_output_details()[0]

    dataset = {}
    for person_dir in sorted(p for p in images_dir.iterdir() if p.is_dir()):
        samples = []
        for image_path in sorted(
            p for p in person_dir.iterdir() if p.suffix.lower() in IMAGE_EXTENSIONS
        ):
            interpreter.set_tensor(input_details["index"], preprocess(image_path)[None, ...])
            interpreter.invoke()
            embedding = interpreter.get_tensor(output_details["index"]).reshape(-1)
            if embedding.size != expected_dim:
                sys.exit(
                    f"Embedding size {embedding.size} != {expected_dim} for {image_path}. "
                    "Use --dim 512 for a 512-dimensional model."
                )
            samples.append((image_path, l2_normalize(embedding)))
        if samples:
            dataset[person_dir.name] = samples

    if len(dataset) < 2:
        sys.exit("Need at least two person folders with images to evaluate ambiguity.")
    return dataset


def recognize(probe: np.ndarray, gallery, threshold: float, margin: float, top_k: int):
    scores = []
    for name, samples in gallery.items():
        distances = sorted(float(np.linalg.norm(probe - sample)) for sample in samples)
        if not distances:
            continue
        scores.append((name, sum(distances[:top_k]) / min(top_k, len(distances))))

    if not scores:
        return "Unknown", 0.0, 0.0, True, False

    scores.sort(key=lambda item: item[1])
    best_name, best_distance = scores[0]
    second_distance = scores[1][1] if len(scores) > 1 else float("inf")
    ambiguous = second_distance < float("inf") and best_distance < threshold and (
        second_distance - best_distance < margin
    )
    accepted = not ambiguous and best_distance < threshold
    return (best_name if accepted else "Unknown", best_distance, second_distance, ambiguous, accepted)


def evaluate(args: argparse.Namespace) -> None:
    dataset = load_embeddings(args.model, args.images, args.dim)
    rows = []

    # Leave-one-out: the image being scored is never part of its own gallery.
    for expected, samples in dataset.items():
        for index, (image_path, embedding) in enumerate(samples):
            gallery = {
                name: [value for other_index, (_, value) in enumerate(items)
                       if name != expected or other_index != index]
                for name, items in dataset.items()
            }
            gallery = {name: values for name, values in gallery.items() if values}
            predicted, best, second, ambiguous, accepted = recognize(
                embedding, gallery, args.threshold, args.margin, args.samples_per_person
            )
            best_candidate = predicted if accepted else min(
                gallery,
                key=lambda name: min(
                    float(np.linalg.norm(embedding - value)) for value in gallery[name]
                ),
            )
            rows.append(
                {
                    "image": str(image_path),
                    "expected": expected,
                    "predicted": predicted,
                    "best_candidate": best_candidate,
                    "best_distance": f"{best:.4f}",
                    "second_distance": "inf" if second == float("inf") else f"{second:.4f}",
                    "ambiguous": ambiguous,
                    "correct": accepted and predicted == expected,
                }
            )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=[
                "image", "expected", "predicted", "best_candidate",
                "best_distance", "second_distance", "ambiguous", "correct",
            ],
        )
        writer.writeheader()
        writer.writerows(rows)

    total = len(rows)
    correct = sum(1 for row in rows if row["correct"])
    unsure = sum(1 for row in rows if row["ambiguous"])
    print(f"Images: {total}  People: {len(dataset)}  Correct: {correct}/{total} ({correct / total:.1%})")
    print(f"UNSURE: {unsure}/{total} ({unsure / total:.1%})  Report: {args.output}")

    # Threshold sweep uses the stored best/second distances from the fixed gallery.
    best_accuracy = (-1.0, args.threshold)
    for step in range(8, 41):
        candidate = step * 0.05
        hits = 0
        for row in rows:
            best = float(row["best_distance"])
            second = float(row["second_distance"])
            ambiguous = second != float("inf") and best < candidate and second - best < args.margin
            if not ambiguous and best < candidate and row["best_candidate"] == row["expected"]:
                hits += 1
        if hits / total > best_accuracy[0]:
            best_accuracy = (hits / total, candidate)
    print(f"Best swept threshold: {best_accuracy[1]:.2f} (accuracy {best_accuracy[0]:.1%})")


if __name__ == "__main__":
    evaluate(parse_args())
