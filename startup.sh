#
# The startup script for RPI
#

# Run NGROK for remote ssh support
/home/pi/ngrok tcp 22 -config /home/pi/config.xml --log=stdout > /home/pi/ngrok.log  &

# Start the scoreboard web app
java -jar /home/pi/scoreboard-1.0.0.jar > /home/pi/output.log &
