from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import base64
import tempfile
import face_recognition
import numpy as np
import os

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

class PersonaRequest(BaseModel):
    imageBase64: str
    uid: str

class PersonaResponse(BaseModel):
    vector: list[float]

@app.post("/renderPersona", response_model=PersonaResponse)
async def render_persona(request: PersonaRequest):
    print(f"[LOG] Received request for UID: {request.uid}")
    
    try:
        # Decode base64 image
        image_bytes = base64.b64decode(request.imageBase64)
        print(f"[LOG] Decoded image bytes: {len(image_bytes)} bytes")

        # Save to temporary file
        with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as temp_image:
            temp_image.write(image_bytes)
            temp_image.flush()
            image_path = temp_image.name
        print(f"[LOG] Saved temp image: {image_path}")

        # Load image using face_recognition
        image = face_recognition.load_image_file(image_path)
        encodings = face_recognition.face_encodings(image)

        if not encodings:
            print(f"[LOG] No faces detected in image: {image_path}")
            raise HTTPException(status_code=400, detail="No face detected. Please try again.")

        vector = encodings[0].tolist()
        print(f"[LOG] Face vector length: {len(vector)}")

        # Optional: clean up temp file
        os.remove(image_path)
        print(f"[LOG] Temp image deleted: {image_path}")

        return {"vector": vector}

    except Exception as e:
        print(f"[ERROR] Processing failed for UID {request.uid}: {e}")
        raise HTTPException(status_code=500, detail=str(e))
