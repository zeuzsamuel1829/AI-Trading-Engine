# AI Trading Engine — Android DEMO

## Estado
Esta entrega contiene una app Android instalable como proyecto de Android Studio.
Incluye:
- Pantalla de acceso local.
- Dashboard móvil.
- Dirección del mercado: alcista / bajista / lateral.
- Fuerza y puntuación.
- Lectura 4H / 1H / 15M / 5M.
- Confluencia de análisis.
- Motor automático simulado.
- Operaciones DEMO simuladas e historial.
- Parada de emergencia.
- Pantalla/botón preparado para futura conexión MT5.

## Importante
Esta versión NO se conecta a MetaTrader 5 y NO envía órdenes reales ni demo reales.
La conexión real requiere un puente externo porque la integración oficial de MetaTrader 5 con Python se realiza mediante el terminal MT5 y funciones de conexión/consulta/orden. La app móvil será el panel de control; el motor y el puente deberán ejecutarse en un servidor o PC/VPS.

## Cómo generar el APK
1. Instala Android Studio.
2. Abre esta carpeta como proyecto.
3. Espera a que Gradle sincronice.
4. Ejecuta `app` en un teléfono Android o usa Build > Build APK(s).
5. Para una versión de distribución, usa Build > Generate Signed APK.

## Próxima etapa
Crear el backend/puente MT5 DEMO:
APP → API segura → motor Python → terminal MT5 → cuenta DEMO.

Nunca introducir credenciales de una cuenta real en esta versión.
