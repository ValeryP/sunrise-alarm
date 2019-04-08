#include <Arduino.h>
#include "RTClib.h"
#include "GyverTM1637.h"
#include "GyverTimer.h"
#include "GyverButton.h"

#define CLK 2
#define DIO 3
#define BUTTON 4
#define RELAY 12

RTC_DS3231 rtc;
GyverTM1637 disp(CLK, DIO);
GButton button(BUTTON);

GTimer_ms check_serial_input(200);
GTimer_ms check_alarm_time(1000);
GTimer_ms check_alarm_state(10);

int8_t alarm_hrs, alarm_mins;
boolean is_alarm;
boolean is_mode_manual_on;

void setup(){
	Serial.begin(9600);
	pinMode(RELAY, OUTPUT);
	digitalWrite(RELAY, LOW);

	rtc.begin();
	if (rtc.lostPower()) {
		rtc.adjust(DateTime(F(__DATE__), F(__TIME__)));
	}

	disp.clear();
	disp.brightness(7);
	disp.point(1);
}

void alert_setup_handler() {
	if (check_serial_input.isReady()) {
		if (Serial.available()) {
			String time = Serial.readString();
			alarm_hrs = time.substring(0,2).toInt();
			alarm_mins = time.substring(2,4).toInt();
			disp.displayClock(alarm_hrs, alarm_mins);
			digitalWrite(RELAY, LOW);
		}
	}
}

void alert_trigger_handler(){
	if (check_alarm_time.isReady()) {
		DateTime now = rtc.now();
		is_alarm = alarm_hrs==now.hour() && alarm_mins==now.minute();
	}
}

void button_monitor_state(){
	button.tick();
	if (button.isClick()) {
		is_mode_manual_on = !is_mode_manual_on;
		is_alarm = !is_alarm;
	}
}

void alert_state_monitor() {
	if (check_alarm_state.isReady()) {
		digitalWrite(RELAY, is_alarm ? HIGH : LOW);
	}
}

void loop(){
	alert_setup_handler();
	button_monitor_state();
	if (!is_mode_manual_on) {
		alert_trigger_handler();
	}
	alert_state_monitor();
}
