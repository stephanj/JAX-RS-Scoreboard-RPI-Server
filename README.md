# Scoreboard Web App

## Use branch spring-boot !!!

    git checkout spring-boot 

## Compile project

Server Scoreboard code moet blijkbaar met JDK 1.7 gecompiled worden om op de versie van TomEE te draaien.

MAAR indien je de 24s MainApp wilt compilen moet dit met JDK 1.8 gebeuren.

Dus de pom.xml moet je telkens aanpassen obv de nodige target JDK indien je MainApp of the server wenst te compilen.

Zie pom.xml

    <configuration>
        <source>1.7</source>
        <target>1.7</target>
    </configuration>

Compile en package project met maven

    mvn clean package

### Run

    mvn tomee:run

### 24 Seconds web client

    http://localhost:8080/

### Maven dependencies

        <openejb.version>4.7.2</openejb.version>
        <tomee.version>1.7.2</tomee.version>

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

## JDK 8 for ARM Downloads

http://www.oracle.com/technetwork/java/javase/downloads/jdk8-arm-downloads-2187472.html
    
    sudo tar zxvf jdk-8u6-linux-arm-vfp-hflt.gz -C /opt

Set default java and javac to the new installed jdk8.

    $ sudo update-alternatives --install /usr/bin/javac javac /opt/jdk1.8.0_06/bin/javac 1
    $ sudo update-alternatives --install /usr/bin/java java /opt/jdk1.8.0_06/bin/java 1

    $ sudo update-alternatives --config javac
    $ sudo update-alternatives --config java

After all, verify with the commands with -version option.

    $ java -version
    $ javac -version

## Creating/Restoring an SD image  

See http://computers.tutsplus.com/articles/how-to-clone-raspberry-pi-sd-cards-using-the-command-line-in-os-x--mac-59911

### Create clone

    diskutil list
    sudo dd if=/dev/disk2 of=~/Desktop/raspberrypi.dmg

### Restore image

    diskutil unmountDisk /dev/disk2
    sudo newfs_msdos -F 16 /dev/disk2
    sudo dd if=~/Desktop/DevoxxSignageRaspberryPI.dmg of=/dev/disk2 bs=1M

Use Ctrl-T to check dd status
