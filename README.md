# columbus-trojan
<img src="https://cloud.githubusercontent.com/assets/7417870/12315404/6dd58120-bab5-11e5-8a10-d5fec03d38d2.gif" width="100" align="right">

This is the Android app for Columbus. It lives on users' Android phones and collects data every three minutes. Data is uploaded only on Wi-Fi.

The app collects the following data:
- Image (front-facing camera)
- 10-second sound clip (microphone)
- Location (mobile triangulation)

The image and sound files are uploaded to an Amazon S3 bucket. When all of the above data has been collected, the app submits a JSON string to the backend server, similar to the following:

```json
{
    "timestamp": 12345,
    "account_id": "abc@gmail.com",
    "location": {
        "lat": 200,
        "lon": 1
    },
    "audio_url": "abc@gmail.com/12345/audio.aac",
    "image_url": "abc@gmail.com/12345/image.jpg"
}
```

`audio_url` and `image_url` - S3 key to the media assets

The app will only use the network if the phone is connected to Wi-Fi. If there is no Wi-Fi connection at the time of data collection, the upload job will be queued for later until Wi-Fi is available.

Libraries used:
- loopj HTTP library
- Google GSON library
- AWS Android SDK
