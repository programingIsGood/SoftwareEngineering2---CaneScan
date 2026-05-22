# CaneScan: Plant Pathogen Detection System

An end-to-end computer vision and database application designed to scan crop images, identify agricultural pathogens using a Custom Convolutional Neural Network (CNN), and log diagnostic results to a Firebase Realtime Database backend.

---

## 🏗️ System Architecture

The core vision model leverages a sequential, deep-feature extraction pipeline optimized for high-resolution agricultural scans.

### Model Structure Summary
* **Feature Extraction:** 4 consecutive Convolutional layers (using $3 \times 3$ kernels and ReLU activations) coupled with Max Pooling layers to progressively downsample spatial resolution while abstracting high-level leaf features.
* **Classification Phase:** A dense flattening layer that routes feature maps into a 128-unit Fully Connected layer, processes the features through a custom MaxPool1d operational layer, and outputs final class probabilities.

---

## 📊 Database Design (ERD)

The system relies on a clean, relational schema designed for efficient diagnostic lookups. The data structure is organized across 5 core entities:

* **Users:** Stores account profiles, hashed credentials, and authentication metrics.
* **Scan Logs:** Tracks geographic data (Latitude/Longitude), image URLs, and user metadata for every scan performed in the field.
* **Diagnostic Results:** Stores the analytical prediction output and the machine learning model's confidence scores.
* **Pathogens:** A reference table mapping scientific and common names of plant diseases.
* **Treatment Recommendations:** Provides action steps and material requirements optimized for specific pathogen IDs.

<img width="1720" height="562" alt="Blank diagram" src="https://github.com/user-attachments/assets/430cfb28-ddcc-4b39-b673-208bda6b88f3" />

📐 **Live Workspace Design Canvas:** [Access the Live Lucidchart Diagram](https://lucid.app/lucidchart/553a10c0-25a7-4def-9c8f-8a9825ebf48d/edit?viewport_loc=1249%2C496%2C2164%2C1091%2C0_0&invitationId=inv_1360d0bf-0bad-4f30-b2d1-b5a500e4bdcb)

---

## 🚀 Getting Started

### 1. Prerequisites & Environment Setup
To ensure reproducible builds across teams, the Android development and SDK environment is fully encapsulated inside a Docker container.

Build the environment image locally:
```bash
docker build -t android-firebase-env .
