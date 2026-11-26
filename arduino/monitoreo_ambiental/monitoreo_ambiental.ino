// ==========================================
// MONITOREO DE AMBIENTE
// CO2 + LUZ + TEMPERATURA  ->  App Android por Bluetooth (HC-05 / HC-06)
// ==========================================
//
// CONEXIÓN DEL MÓDULO BLUETOOTH:
//   HC-05 VCC -> 5V
//   HC-05 GND -> GND
//   HC-05 TXD -> pin 2 del Arduino (directo)
//   HC-05 RXD <- pin 3 del Arduino A TRAVÉS de un divisor de voltaje:
//        pin 3 ---[ 1 kΩ ]---+--- RXD del HC-05
//                            |
//                         [ 2 kΩ ]
//                            |
//                           GND
//   (el RXD del módulo trabaja a 3.3V; el divisor baja los 5V del Arduino)
//
// Se usan los pines 2 y 3 (y no 0 y 1) para que el módulo no interfiera
// al subir el código, y para seguir usando el Monitor Serie.
//
// MENSAJE QUE SE ENVÍA A LA APP (una línea por segundo):
//   CO2:512;LUZ:300;TEMP:22
// ==========================================

#include <SoftwareSerial.h>

// --- Bluetooth ---
const int BT_RX = 2;   // recibe lo que manda el módulo (va al TXD del HC-05)
const int BT_TX = 3;   // envía al módulo (va al RXD del HC-05 con divisor)
SoftwareSerial bluetooth(BT_RX, BT_TX);

// --- Pines de entrada (potenciómetros que simulan los sensores) ---
const int PIN_CO2  = A0;   // simula MQ-135
const int PIN_LUZ  = A1;   // simula KY-018
const int PIN_TEMP = A2;   // simula sensor de temperatura

// --- Pines de salida (LEDs de alerta) ---
const int LED_CO2  = 13;
const int LED_LUZ  = 12;
const int LED_TEMP = 11;

// --- Umbrales ---
// IMPORTANTE: son los mismos rangos que usa la app (Sensores.kt).
// Si cambian uno, cambien el otro.
const int UMBRAL_CO2       = 600;  // alerta si la lectura es MAYOR
const int UMBRAL_LUZ_BAJA  = 300;  // alerta si la lectura es MENOR
const int UMBRAL_TEMP_BAJA = 15;   // alerta si es menor (°C)
const int UMBRAL_TEMP_ALTA = 30;   // alerta si es mayor (°C)

// Cada cuánto se lee y se envía (milisegundos)
const unsigned long INTERVALO = 1000;


// Enciende un LED medio segundo para comprobar que está bien conectado
void probarLed(int pin, const char* nombre) {
  Serial.print("Probando LED de ");
  Serial.println(nombre);
  digitalWrite(pin, HIGH);
  delay(500);
  digitalWrite(pin, LOW);
  delay(200);
}


void setup() {
  Serial.begin(9600);      // Monitor Serie (computador)
  bluetooth.begin(9600);   // HC-05 / HC-06 (velocidad de fábrica: 9600)
  delay(500);

  Serial.println();
  Serial.println("=== Monitoreo ambiental iniciado ===");

  pinMode(LED_CO2, OUTPUT);
  pinMode(LED_LUZ, OUTPUT);
  pinMode(LED_TEMP, OUTPUT);

  // Prueba de LEDs al encender
  probarLed(LED_CO2, "CO2");
  probarLed(LED_LUZ, "Luz");
  probarLed(LED_TEMP, "Temperatura");

  Serial.println("Enviando datos por Bluetooth cada segundo...");
  Serial.println();
}


void loop() {
  // --- Leer potenciómetros (0 a 1023) ---
  int valorCO2  = analogRead(PIN_CO2);
  int valorLuz  = analogRead(PIN_LUZ);
  int valorTemp = analogRead(PIN_TEMP);

  // --- Convertir la lectura de temperatura a 0–50 °C (número entero, igual que en la app) ---
  int temperatura = round(valorTemp * 50.0 / 1023.0);

  // --- Evaluar condiciones ---
  bool alertaCO2  = valorCO2 > UMBRAL_CO2;
  bool alertaLuz  = valorLuz < UMBRAL_LUZ_BAJA;
  bool alertaTemp = temperatura < UMBRAL_TEMP_BAJA || temperatura > UMBRAL_TEMP_ALTA;

  // --- LEDs ---
  digitalWrite(LED_CO2,  alertaCO2  ? HIGH : LOW);
  digitalWrite(LED_LUZ,  alertaLuz  ? HIGH : LOW);
  digitalWrite(LED_TEMP, alertaTemp ? HIGH : LOW);

  // --- Enviar a la app por Bluetooth:  CO2:512;LUZ:300;TEMP:22 ---
  bluetooth.print("CO2:");
  bluetooth.print(valorCO2);
  bluetooth.print(";LUZ:");
  bluetooth.print(valorLuz);
  bluetooth.print(";TEMP:");
  bluetooth.println(temperatura);

  // --- Mostrar lo mismo en el Monitor Serie (para revisar) ---
  Serial.print("Enviado -> CO2:");
  Serial.print(valorCO2);
  Serial.print(alertaCO2 ? " [ALERTA]" : " [ok]");
  Serial.print("  LUZ:");
  Serial.print(valorLuz);
  Serial.print(alertaLuz ? " [ALERTA]" : " [ok]");
  Serial.print("  TEMP:");
  Serial.print(temperatura);
  Serial.println(alertaTemp ? " C [ALERTA]" : " C [ok]");

  delay(INTERVALO);
}
