const functions = require('firebase-functions');
const axios = require("axios");

/**
 * Ssend a message to the Android app via FCM and switch the light
 *
 * @param response object responsible for sending the data
 * @param isLightOn mode of the switch (lights on, lights off)
 */
function sendFCM(response, isLightOn) {
    const postData = {
        "to": "/topics/light",
        "collapse_key": "type_a",
        "data": {
            "light": Boolean(isLightOn)
        }
    };
    const config = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': functions.config().header.authorization,
        }
    };
    axios.post('https://fcm.googleapis.com/fcm/send', postData, config)
        .then((res) => {
            console.log("RESPONSE RECEIVED: ", res);
            response.send(res);
        })
        .catch((err) => {
            console.log("AXIOS ERROR: ", err);
            response.send(err);
        })
}

// Function triggered externally to send a message to the Android app
exports.send_fcm_light_on = functions.https.onRequest((_, response) => sendFCM(response, true));
exports.send_fcm_light_off = functions.https.onRequest((_, response) => sendFCM(response, false));
