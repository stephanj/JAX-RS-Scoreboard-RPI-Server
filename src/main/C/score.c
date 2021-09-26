/*********************************************
 *
 * SPI to MAX7221 (using spidev driver)
 *
 * Compile on PI using: gcc -oscore score.c -lm
 *
 *********************************************/

#include <math.h>
#include <stdint.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <getopt.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/time.h>
#include <linux/types.h>
#include <linux/spi/spidev.h>

void timestamp(const char *s)
{
   time_t curtime;
   struct tm *loctime;
   char timestr [20];

   curtime = time(NULL);
   loctime = localtime(&curtime);
   strftime(timestr,20,"%H:%M.%S",loctime);

    printf("%s - %s",timestr,s);
}

static void pabort(const char *s)
{
   perror(s);
   abort();
}

static const char *device0 = "/dev/spidev0.0";
static const char *device1 = "/dev/spidev0.1";
static uint8_t mode;
static uint8_t bits = 8;
static uint32_t speed = 200000;
static uint16_t delay;

static void matrixwrite(int fd, unsigned char max_address, unsigned char max_data)
{
   uint8_t tx[] = { max_address, max_data, };
   write(fd, tx, 2);
}

static void initspi(int fd)
{
   int ret = 0;

   /** spi mode **/
   ret = ioctl(fd, SPI_IOC_WR_MODE, &mode);
   if (ret == -1)
      pabort("can't set spi mode");

   ret = ioctl(fd, SPI_IOC_RD_MODE, &mode);
   if (ret == -1)
      pabort("can't get spi mode");

   /** bits per word **/
   ret = ioctl(fd, SPI_IOC_WR_BITS_PER_WORD, &bits);
   if (ret == -1)
      pabort("can't set bits per word");

   ret = ioctl(fd, SPI_IOC_RD_BITS_PER_WORD, &bits);
   if (ret == -1)
      pabort("can't get bits per word");

   /** max speed hz **/
   ret = ioctl(fd, SPI_IOC_WR_MAX_SPEED_HZ, &speed);
   if (ret == -1)
      pabort("can't set max speed hz");

   ret = ioctl(fd, SPI_IOC_RD_MAX_SPEED_HZ, &speed);
   if (ret == -1)
      pabort("can't get max speed hz");

	// Initialize Matrix
    matrixwrite(fd, 0x0F, 0x00); // Write 0X00 to get it out of Display Test Mode.
	matrixwrite(fd, 0x0C, 0x01); // 0x01 Normal operation - 0x00 for shutdown
	matrixwrite(fd, 0x0B, 0x07); // Scan Limit (scan all digits)
	matrixwrite(fd, 0x0A, 0x07); // Intensity 0x01 (LOW) .. 0x0F (high)
	matrixwrite(fd, 0x09, 0xFF); // Decode mode (on!)
}

void led_counter(int fd, int delay)
{
   uint8_t col;
   uint8_t value;
   uint8_t z;

   timestamp("2. Counter\n");

   for (col = 1; col <= 8; col++) {
	value = col - 1;
   		matrixwrite(fd, col, 8);
   }

    usleep(delay);
}

void all_leds_off(int fd) {
   uint8_t col;

   matrixwrite(fd, 0x09, 0x00); // Decode mode (off)

   for (col = 1; col <= 8; col++) {
	 matrixwrite(fd, col, 0x00);
   }
}

void all_leds_on(int fd) {
   uint8_t col;

   matrixwrite(fd, 0x09, 0x00); // Decode mode (off)

   for (col = 1; col <= 8; col++) {
	 matrixwrite(fd, col, 0xFF);
   }
}

void usage(void)
{
	printf("Usage:\n");
	printf(" -hnnn score 'h'ome team\n");
	printf(" -vnnn score 'v'isitor team\n");
	printf(" -k mmss (full time excluding 24 secs)\n");
	printf(" -m mm (minutes)\n");
	printf(" -s ss (seconds)\n");
	printf(" -t tt (24 seconds)\n");
	printf(" -q n (quarter 1..4)\n");
	printf(" -a n (foul team A 0..5)\n");
	printf(" -b n (foul team B 0..5)\n");
	printf(" -f 123456789012345 (set full scoreboard)\n");
	printf(" -z clear all leds\n");
	printf(" -x test\n");
	printf(" \n");
	printf("Application developed by Stephan Janssen (sja@devoxx.com)\n");
	printf("Sept 2013 - v0.1.1\n");

	exit (8);
}

int main(int argc, char *argv[])
{
	unsigned char score1;
	unsigned char score2;
	unsigned char score3;

	unsigned char home1;
	unsigned char home2;
	unsigned char home3;

	unsigned char visitors1;
	unsigned char visitors2;
	unsigned char visitors3;

	unsigned char twemtyfour1;
	unsigned char twemtyfour2;

	unsigned char clock;

	unsigned char minute1;
	unsigned char minute2;

	unsigned char seconds1;
	unsigned char seconds2;

	unsigned char quarter;
	unsigned char foul;
	unsigned char foulA;
	unsigned char foulB;

	int fd0;
	int fd1;

	if (argc == 1) {
	    usage();
	    exit(0);
	}

	while ((argc > 1) && (argv[1][0] == '-'))
	{
		switch (argv[1][1])
		{
			case 'h':					// score home
				if (sscanf(&argv[1][2], "%c", &score1) != 1) {
					printf("Wrong -hXnn value, failed.\n");
					exit(-2);
				} else if (sscanf(&argv[1][3], "%c", &score2) != 1) {
					printf("Wrong -hnXn value, failed.\n");
					exit(-2);
				} else if (sscanf(&argv[1][4], "%c", &score3) != 1) {
					printf("Wrong -hnnX value, failed.\n");
					exit(-2);
				}

				fd1 = open(device1, O_RDWR);

			    if (fd1 < 0) {
      				pabort("can't open device");
                }

   				initspi(fd1);

				matrixwrite(fd1, 2, score2);
				matrixwrite(fd1, 3, score3);

				close(fd1);

                fd0 = open(device0, O_RDWR);
                if (fd0 < 0) {
                    pabort("can't open device");
                }

                initspi(fd0);

                if (score1 == '1') {
                    matrixwrite(fd0, 4, score1);
                } else {
                    matrixwrite(fd0, 4, '6');
                }

				close(fd0);

				break;

			case 'v':					// score visitors
				if (sscanf(&argv[1][2], "%c", &score1) != 1) {
					printf("Wrong -vXnn value, failed.\n");
					exit(-2);
				} else if (sscanf(&argv[1][3], "%c", &score2) != 1) {
					printf("Wrong -vnXn value, failed.\n");
					exit(-2);
				} else if (sscanf(&argv[1][4], "%c", &score3) != 1) {
					printf("Wrong -vnnX value, failed.\n");
					exit(-2);
				}

				fd1 = open(device1, O_RDWR);
			    if (fd1 < 0) {
      				pabort("can't open device");
                }

   				initspi(fd1);

				matrixwrite(fd1, 5, score2);
				matrixwrite(fd1, 6, score3);

				close(fd1);

                fd0 = open(device0, O_RDWR);
                if (fd0 < 0) {
                    pabort("can't open device");
                }

                initspi(fd0);
                if (score1 == '1') {
                    matrixwrite(fd0, 5, score1);
                } else {
                    matrixwrite(fd0, 5, '6');
                }

                close(fd0);
				break;

			case 'm':					// minutes
				if (sscanf(&argv[1][2], "%c", &minute1) != 1) {
					printf("Wrong -mXn value, failed.\n");
					exit(-2);
				} else if (sscanf(&argv[1][3], "%c", &minute2) != 1) {
					printf("Wrong -mnX value, failed.\n");
					exit(-2);
				}

				// printf("minutes : %c %c\n", minute1, minute2);

				fd1 = open(device1, O_RDWR);
			    if (fd1 < 0) {
      				pabort("can't open device");
                }

   				initspi(fd1);

				matrixwrite(fd1, 1, minute1);
				matrixwrite(fd1, 4, minute2);

				close(fd1);
				break;

			case 's':					// seconds
				if (sscanf(&argv[1][2], "%c", &seconds1) != 1) {
					printf("Wrong -sXn value, failed.\n");
					exit(-2);
				} else if (sscanf(&argv[1][3], "%c", &seconds2) != 1) {
					printf("Wrong -snX value, failed.\n");
					exit(-2);
				}

				fd0 = open(device0, O_RDWR);
			    if (fd0 < 0) {
      				pabort("can't open device");
                }

   				initspi(fd0);

				matrixwrite(fd0, 6, seconds1);
				matrixwrite(fd0, 7, seconds2);

				close(fd0);
				break;

			case 't':					// 24 seconds
				if (sscanf(&argv[1][2], "%c", &twemtyfour1) != 1) {
					printf("Wrong -tXn value, failed.\n");
					exit(-2);
				} else if (sscanf(&argv[1][3], "%c", &twemtyfour2) != 1) {
					printf("Wrong -tnX value, failed.\n");
					exit(-2);
				}

				fd1 = open(device1, O_RDWR);
			    if (fd1 < 0) {
      				pabort("can't open device");
                }

   				initspi(fd1);

				matrixwrite(fd1, 7, twemtyfour1);
				matrixwrite(fd1, 8, twemtyfour2);

				close(fd1);
				break;

			case 'q':					// quarter
				if (sscanf(&argv[1][2], "%c", &quarter) != 1) {
					printf("Wrong -qX value, failed.\n");
					exit(-2);
				}

				// printf("quarter : %c\n", quarter);

				fd0 = open(device0, O_RDWR);
			    if (fd0 < 0) {
      				pabort("can't open device");
                }

   				initspi(fd0);

				matrixwrite(fd0, 2, quarter);

				close(fd0);
				break;

			case 'a':					// Fouls team A
				if (sscanf(&argv[1][2], "%c", &foul) != 1) {
					printf("Wrong -aX value, failed.\n");
					exit(-2);
				}

				fd0 = open(device0, O_RDWR);
			    if (fd0 < 0) {
      				pabort("can't open device");
                }

   				initspi(fd0);

				matrixwrite(fd0, 1, foul);

				close(fd0);
				break;

			case 'b':					// Fouls team B
				if (sscanf(&argv[1][2], "%c", &foul) != 1) {
					printf("Wrong -aX value, failed.\n");
					exit(-2);
				}

				fd0 = open(device0, O_RDWR);
			    if (fd0 < 0) {
      				pabort("can't open device");
                }

   				initspi(fd0);

				matrixwrite(fd0, 3, foul);

				close(fd0);
				break;

			case 'f':					// Full Scoreboard

				fd0 = open(device0, O_RDWR);
			    if (fd0 < 0) {
      				pabort("can't open device");
                }

   				initspi(fd0);

                // 1) Score Home (3 digits)
                sscanf(&argv[1][2], "%c", &home1);
                sscanf(&argv[1][3], "%c", &home2);
                sscanf(&argv[1][4], "%c", &home3);

                // 2) Score Visitors (3 digits)
                sscanf(&argv[1][5], "%c", &visitors1);
                sscanf(&argv[1][6], "%c", &visitors2);
                sscanf(&argv[1][7], "%c", &visitors3);

                // 3) # timeouts Home (1 digit)
                // 4) # timeouts Visitors (1 digit)

                // 5) # fouls Home (1 digit)
                sscanf(&argv[1][10], "%c", &foulA);

                // 6) # fouls Visitors (1 digit)
                sscanf(&argv[1][11], "%c", &foulB);

                // 7) minutes clock (2 digits)
                sscanf(&argv[1][12], "%c", &minute1);
                sscanf(&argv[1][13], "%c", &minute2);

                // 8) seconds clock (2 digits)
                sscanf(&argv[1][14], "%c", &seconds1);
                sscanf(&argv[1][15], "%c", &seconds2);

                // 9) quarter (1 digit)
                sscanf(&argv[1][16], "%c", &quarter);

                matrixwrite(fd0, 1, foulA);      // Fouls A
                matrixwrite(fd0, 2, quarter);   // Quarter
                matrixwrite(fd0, 3, foulB);      // Fouls B
				matrixwrite(fd0, 4, minute1);   // Minute 1
				matrixwrite(fd0, 5, minute2);   // Minute 2
				matrixwrite(fd0, 6, seconds1);  // Seconds 1
				matrixwrite(fd0, 7, seconds2);  // Seconds 2

				close(fd0);

				fd1 = open(device1, O_RDWR);
			    if (fd1 < 0) {
      				pabort("can't open device");
                }

   				initspi(fd1);

   				// Score
				matrixwrite(fd1, 2, home2);
				matrixwrite(fd1, 3, home3);
				matrixwrite(fd1, 5, visitors2);
				matrixwrite(fd1, 6, visitors3);

                if (home1 == '1') {
                    matrixwrite(fd1, 1, score1);
                } else {
                    matrixwrite(fd1, 0x09, 0x00); // Decode mode (off)
                    matrixwrite(fd1, 1, 0x00);
                }

                if (visitors1 == '1') {
				    matrixwrite(fd1, 4, visitors1);
                } else {
                    matrixwrite(fd1, 0x09, 0x00); // Decode mode (off)
                    matrixwrite(fd1, 4, 0x00);
                }

				close(fd1);
				break;

			case 'k':					// Full time excl. 24 seconds
				if (sscanf(&argv[1][2], "%c", &minute1) != 1) {
					printf("Wrong -fXnnnnn value, failed.\n");
					exit(-2);
				} else if (sscanf(&argv[1][3], "%c", &minute2) != 1) {
					printf("Wrong -fnXnnnn value, failed.\n");
					exit(-2);
				} else if (sscanf(&argv[1][4], "%c", &seconds1) != 1) {
					printf("Wrong -fnnXnnn value, failed.\n");
					exit(-2);
				} else if (sscanf(&argv[1][5], "%c", &seconds2) != 1) {
					printf("Wrong -fnnnXnn value, failed.\n");
					exit(-2);
				}

				fd1 = open(device1, O_RDWR);
			    if (fd1 < 0) {
      				pabort("can't open device");
                }
                initspi(fd1);
				matrixwrite(fd1, 1, minute1);
				matrixwrite(fd1, 4, minute2);
                close(fd1);

				fd0 = open(device0, O_RDWR);
			    if (fd0 < 0) {
      				pabort("can't open device");
                }
				initspi(fd0);
                matrixwrite(fd0, 6, seconds1);
				matrixwrite(fd0, 7, seconds2);

				close(fd0);
				break;

			case 'z':					// clear all
				fd0 = open(device0, O_RDWR);
			    if (fd0 < 0) {
      				pabort("can't open device");
                }

   				initspi(fd0);
				all_leds_off(fd0);
				close(fd0);

				fd1 = open(device1, O_RDWR);
			    if (fd1 < 0) {
      				pabort("can't open device");
                }

   				initspi(fd1);
				all_leds_off(fd1);
				close(fd1);
				break;

			case 'x':					// test
				fd0 = open(device0, O_RDWR);
			    if (fd0 < 0) {
      				pabort("can't open device");
                }

   				initspi(fd0);
				all_leds_on(fd0);
				close(fd0);

				fd1 = open(device1, O_RDWR);
			    if (fd1 < 0) {
      				pabort("can't open device");
                }

   				initspi(fd1);
				all_leds_on(fd1);
				close(fd1);

				break;

			default:
				printf("Wrong Argument: %s\n", argv[1]);
				usage();
		}

		++argv;
		--argc;
	}
   return;
}
