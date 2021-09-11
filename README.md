# Scoreboard Web App

## Project Source Code

SD card images and extra documentation is available on Google Drive @
https://drive.google.com/drive/folders/0B_I8gQQT3G8Zd1ZXa3ZvbXhVQnM?resourcekey=0-AU8u-hJuU9W9eUqC1OxRZg&usp=sharing

### Spring Boot Scoreboard

This is the new Spring Boot version of the scoreboard using Java 11.

    https://github.com/stephanj/JAX-RS-Scoreboard-RPI-Server

### OBS "Get Score" Project

This Spring Boot app gets the game info from Court B and parses the JSON response into separate txt files.
OBS picks up the txt files to show the scores, clock and 24s as text overlays on top of the video stream.

    https://github.com/stephanj/get-score-for-obs/tree/master

### TomEE Scoreboard

This is the old version of the scoreboard using TomEE. 

    https://bitbucket.org/stephanj/jax-rs-scoreboard-server

## Compile project

The updated scoreboard project now uses Java 11 and Spring Boot. 

Compile and package project using maven with following command :

    mvn clean package

### Run Spring Boot app

    mvn spring-boot:run 

### 24 Seconds web client

    http://localhost:8080/

### Install software on RPI

Get source code from github

    git clone https://github.com/stephanj/JAX-RS-Scoreboard-RPI-Server.git 

    cd JAX-RS-Scoreboard-RPI-Server

    mvn clean compile package 

    cp target/scoreboard.1.0.0.jar /home/pi/.

### REST METHODS

#### GAME

    POST http://localhost:8080/api/game/                    ->      Response createGame(String, String, int)
    GET http://localhost:8080/api/game/{id}                 ->      Game showGame(long)
    GET http://localhost:8080/api/game/list                 ->      List<Game> listGames(int, int)
    DELETE http://localhost:8080/api/game/{id}              ->      Response deleteGame(long)

#### CLOCK
    
    GET http://localhost:8080/api/clock/{gameId}            ->      Response getClock(long)
    PUT http://localhost:8080/api/clock/dec/{gameId}        ->      Response decClock(long)
    PUT http://localhost:8080/api/clock/inc/{gameId}        ->      Response incClock(long)
    PUT http://localhost:8080/api/clock/start/{gameId}      ->      Response startClock(long)
    PUT http://localhost:8080/api/clock/stop/{gameId}       ->      Response stopClock(long)

#### 24 SECONDS CLOCK
    
    PUT http://localhost:8080/api/twentyfour/start/{gameId} ->      Response startClock(long)
    PUT http://localhost:8080/api/twentyfour/stop/{gameId}  ->      Response stopClock(long)
    GET http://localhost:8080/api/twentyfour/reset/{gameId} ->      Response resetClock(long)
    GET http://localhost:8080/api/twentyfour/{gameId}       ->      Response getClock(long)
    PUT http://localhost:8080/api/twentyfour/dec/{gameId}   ->      Response decClock(long)
    PUT http://localhost:8080/api/twentyfour/inc/{gameId}   ->      Response incClock(long)

#### FOULS

    GET http://localhost:8080/api/foul/{teamId}             ->      Response getFouls(long)
    PUT http://localhost:8080/api/foul/dec/{teamId}         ->      Response decrementFouls(long)
    PUT http://localhost:8080/api/foul/inc/{teamId}         ->      Response incrementFouls(long)
    PUT http://localhost:8080/api/foul/reset/{gameId}       ->      Response resetFouls(long)

#### QUARTER

    GET http://localhost:8080/api/quarter/{gameId}          ->      Response getQuarter(long)
    PUT http://localhost:8080/api/quarter/dec/{gameId}      ->      Response decrementQuarter(long)
    PUT http://localhost:8080/api/quarter/inc/{gameId}      ->      Response incrementQuarter(long)

#### SCORE

    GET http://localhost:8080/api/score/{teamId}            ->      Response getScore(long)
    PUT http://localhost:8080/api/score/dec/{teamId}        ->      Response decrementScore(long, int)
    PUT http://localhost:8080/api/score/inc/{teamId}        ->      Response incrementScore(long, int)

#### UTIL

    GET http://localhost:8080/api/util/clear                ->      Response clearGameboard()
    GET http://localhost:8080/api/util/redraw               ->      Response redrawGameboard()
    PUT http://localhost:8080/api/util/clocks/stop          ->      Response stopClocks()
    PUT http://localhost:8080/api/util/tweet/{gameId}       ->      Response tweetGame()

# Raspberry Pi info 

## JDK 11 for ARM Downloads

Use the Desktop to Add/Remove programs to the Debian install.  Here you can search on "OpenJDK" and select the 
Java 11 headless package with JRE. 

## Creating/Restoring an SD image  

See also http://computers.tutsplus.com/articles/how-to-clone-raspberry-pi-sd-cards-using-the-command-line-in-os-x--mac-59911

### Create image (clone)

    diskutil list
    sudo dd if=/dev/disk4 of=~/Desktop/RPI4-KBBCO.dmg

Use Ctrl-T to check dd status

### Restore image

    diskutil unmountDisk /dev/disk4
    sudo newfs_msdos -F 16 /dev/disk4
    sudo dd if=~/Desktop/RPI4-KBBCO.dmg of=/dev/disk4 bs=1m

Use Ctrl-T to check dd status


# Automatically execute script at Linux startup with Debian 9 (Stretch)

Create a file name /home/pi/startup.sh with the following command

    java -jar /home/pi/JAX-RS-Scoreboard-RPI-Server/target/scoreboard-0.0.1-SNAPSHOT.jar

Now make sure the script is executable

    $ sudo chmod +x /home/pi/startup.sh

Add script to file /etc/rc.local :

    #!/bin/sh -e
    #
    # rc.local
    #
    # This script is executed at the end of each multiuser runlevel.
    # Make sure that the script will "exit 0" on success or any other
    # value on error.
    #
    # In order to enable or disable this script just change the execution
    # bits.
    #
    # By default this script does nothing.
    
    # added by ADMIN to run fancy stuff at boot:
    /home/pi/startup.sh || exit 1
    
    exit 0

Make sure /etc/rc.local is executable:

    $ sudo chmod +x /etc/rc.local

Test that your script gets executed if rc.local is started:
    
    $ sudo service rc.local restart

# RPI Network Config 

The RPI is connected with an ethernet cable to the local network.  
Wifi is not used by the RPI so doesn't need to be configured.

Edit /etc/dhcpcd.conf and add the following entry:

    interface eth0
    static ip_address=192.168.1.100
    static routers=192.168.11.1
    static domain_name_servers=8.8.8.8

Only the static ip_address is import and is defined based on the basketball court.
Court A = 192.168.1.100
Court B = 192.168.1.101
Court C = 192.168.1.102


# Reverse SSH using Gnork 

See also https://www.endtoend.ai/tutorial/ngrok-ssh-forwarding/ 

## Authenticate ngrok

To use ngrok, you must be first authenticate it using the ngrok executable file from the unzipped folder.

    # If you installed with snap
    ngrok authtoken <YOUR_AUTH_TOKEN>
    # If you downloaded a zipped file
    ./ngrok authtoken <YOUR_AUTH_TOKEN>

Your authentication token can be found in the dashboard page of ngrok.

## Run ngrok Server

Now, we can start forwarding the SSH port using ngrok! Run the following command:

    # If you installed with snap
    ngrok tcp 22
    # If you downloaded a zipped file
    ./ngrok tcp 22

You should see some output similar to this:

    ngrok by @inconshreveable                                                   (Ctrl+C to quit)
    
    Session Status                online
    Account                       <YOUR_EMAIL> (Plan: <YOUR_PLAN>)
    Version                       2.3.35
    Region                        Japan (jp)
    Web Interface                 http://127.0.0.1:4040
    Forwarding                    <YOUR_ASSIGNED_URL> -> localhost:22

Your SSH port is now accessible in what is written in <YOUR_ASSIGNED_URL>! For example, if it says tcp://0.tcp.jp.ngrok.io:11111, you can access it using that URL.

### Connect to RPI using ssh

You have successfully set up SSH access to your remote Linux machine using ngrok. You can SSH into the machine using the following command:

    # Assuming your URL was tcp://0.tcp.jp.ngrok.io:11111
    ssh <YOUR_USERNAME>@0.tcp.jp.ngrok.io -p 11111

    # Using the RPI username pi
    ssh pi@0.tcp.jp.ngrok.io -p 11111
