#include <Servo.h>

// defines pins numbers
const int stepPin = 3;
const int dirPin = 4;

Servo servoLeft, servoRight;

bool isMoving = false;

void setup() {
  pinMode(LED_BUILTIN, OUTPUT);
  pinMode(stepPin, OUTPUT);
  pinMode(dirPin, OUTPUT);
  Serial.begin(9600);
  digitalWrite(LED_BUILTIN, LOW);
  servoLeft.attach(9);
  servoRight.attach(10);
  servoLeft.write(91);
  servoRight.write(89);
  digitalWrite(dirPin, HIGH);
}

void loop() {
  char inChar = (char)Serial.read();
  switch (inChar) {
    case 60:
      isMoving = false;
      break;
    case 61:
      isMoving = true;
      break;

    case 70:
      servoLeft.write(60);
      servoRight.write(60);
      break;
    case 71:
      servoLeft.write(91);
      servoRight.write(91);
      break;
    case 72:
      servoLeft.write(120);
        servoRight.write(120);
      break;

    case 80:
      servoRight.write(60);
      servoLeft.write(60);
      break;
    case 81:
      servoRight.write(91);
        servoLeft.write(91);
      break;
    case 82:
      servoRight.write(120);
      servoLeft.write(120);
      break;
    default:
      // noop
      break;
  }

  move();

  delay(10);
}

void move() {
  if (isMoving) {
    for (int x = 0; x < 200; x++) {
      digitalWrite(stepPin, HIGH);
      delayMicroseconds(500);
      digitalWrite(stepPin, LOW);
      delayMicroseconds(500);
    }
  }
}
