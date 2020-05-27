#include <Arduino.h>
#include "RTClib.h"
#include "GyverTM1637.h"
#include "GyverTimer.h"
#include "GyverButton.h"

// LCD display pins
#define CLK 2
#define DIO 3

// button for the manual control
#define BUTTON 4

// relay to control external power 
#define RELAY 12

// real time clock to keep traking time after reboot
RTC_DS3231 rtc;

// TM1637 LCD
GyverTM1637 disp(CLK, DIO);

// button for manual control
GButton button(BUTTON);

// timers to optimise the monitoring
GTimer_ms check_serial_input(200);
GTimer_ms check_alarm_time(1000);
GTimer_ms check_alarm_state(10);

// state management variables
int8_t alarm_hrs, alarm_mins;
boolean is_alarm;
boolean is_mode_manual_on;

// initializes and sets the initial values
void setup(){
	Serial.begin(9600);
	pinMode(RELAY, OUTPUT);
	digitalWrite(RELAY, LOW);

	rtc.begin();
	// re-sync the RTC on start 
	if (rtc.lostPower()) {
		rtc.adjust(DateTime(F(__DATE__), F(__TIME__)));
	}

	disp.clear();
	disp.brightness(7);
	disp.point(1);
}

// loops consecutively
void loop(){
	alert_setup_handler();
	button_monitor_state();
	// don't use alarm for manual mode (when turned on the light)
	if (!is_mode_manual_on) { 
		alert_trigger_handler();
	}
	alert_state_monitor();
}

// checks serial, read alarm time and display it on the LCD screen
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

// defined the state of alarm based on the time range
void alert_trigger_handler(){
	if (check_alarm_time.isReady()) {
		DateTime now = rtc.now();
		is_alarm = alarm_hrs==now.hour() && alarm_mins==now.minute();
	}
}

// switch the mode once the button click detected
void button_monitor_state(){
	button.tick();
	if (button.isClick()) {
		is_mode_manual_on = !is_mode_manual_on;
		is_alarm = !is_alarm;
	}
}

// switch the mode once the alarm is triggered
void alert_state_monitor() {
	if (check_alarm_state.isReady()) {
		digitalWrite(RELAY, is_alarm ? HIGH : LOW);
	}
}