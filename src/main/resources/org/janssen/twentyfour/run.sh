# Place this on the 24s Raspberry PI
#
# ~/twentyfour/classes
#
# ~/twentyfour/lib $ ls -l
#  -rw-r----- 1 pi pi  60686 Dec 14 12:43 commons-logging-1.1.1.jar
#  -rw-r--r-- 1 pi pi 289954 Dec 14 12:36 httpclient-4.0.jar
#  -rw-r--r-- 1 pi pi 172888 Dec 14 12:38 httpcore-4.0.1.jar
#  -rw-r--r-- 1 pi pi 170200 Dec 14 12:39 pi4j-core-0.0.5.jar
#

sudo rm /home/pi/twentyFour.bak
sudo mv /home/pi/twentyFour.log /home/pi/twentyFour.log.bak

ip addr show

sudo java -classpath /home/pi/twentyfour/classes:/home/pi/twentyfour/lib/httpclient-4.0.jar:/home/pi/twentyfour
/lib/httpcore-4.0.1.jar:/home/pi/twentyfour/lib/pi4j-core-0.0.5.jar:/home/pi/twentyfour/lib/commons-logging-1.1
.1.jar org.janssen.twentyfour.MainApp >> /home/pi/twentyFour.log