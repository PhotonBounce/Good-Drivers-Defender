import cv2
import os

video_path = r"d:\video.mp4"
output_dir = r"C:\Users\fucktrumpandrednecks\.gemini\antigravity-ide\brain\2f3fb355-c86c-4b43-8d25-559fa67796ab"
output_path = os.path.join(output_dir, "real_frame.png")

if not os.path.exists(output_dir):
    os.makedirs(output_dir)

print(f"Opening video: {video_path}")
cap = cv2.VideoCapture(video_path)
if not cap.isOpened():
    print("Error: Could not open video.")
    exit(1)

total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
fps = cap.get(cv2.CAP_PROP_FPS)
print(f"Total frames: {total_frames}, FPS: {fps}")

# Read the middle frame
target_frame = int(total_frames * 0.5)
cap.set(cv2.CAP_PROP_POS_FRAMES, target_frame)

ret, frame = cap.read()
if ret:
    cv2.imwrite(output_path, frame)
    print(f"Successfully saved frame to {output_path}")
else:
    print("Error reading middle frame. Trying first frame...")
    cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
    ret, frame = cap.read()
    if ret:
        cv2.imwrite(output_path, frame)
        print("Successfully saved first frame")
    else:
        print("Error: Could not read any frame.")

cap.release()
