import csv
import matplotlib.pyplot as plt

x = []
run_time = []
air_thr_pos = []
thr_pos = []
rpm = []
speed = []
coolant_temp = []

def read_csv(filename):
    with open(filename) as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        count = 0
        for row in csv_reader:
            if (count == 0):
                count += 1
            else:
                x.append(count)
                run_time.append(float(row[0]))
                air_thr_pos.append(float(row[1]))
                thr_pos.append(float(row[2]))
                rpm.append(float(row[3]))
                speed.append(float(row[4]))
                coolant_temp.append(float(row[5]))
                count += 1

def make_plot():
#    plt.plot(x, y, 'ro', x, y)
    plt.subplot(211)
    plt.plot(x, run_time, 'r', label='Run Time')
    plt.plot(x, rpm, 'm', label='Engine RPM')
    plt.legend(loc='upper left')
    plt.subplot(212)
    plt.plot(x, air_thr_pos, 'b', label='Air Temp')
    plt.plot(x, thr_pos, 'y', label='Throttle Pos')
    plt.plot(x, speed, 'c', label='Speed')
    plt.plot(x, coolant_temp, 'k', label='Coolant Temp')
    plt.ylabel('value')
    plt.xlabel('sample')
    plt.title('V6386 - 8/4 21:30')
    plt.legend(loc='upper left')
    plt.show()

if __name__ == "__main__":
    try:
        read_csv("../../log/vehicle/V6386.csv")
        make_plot()
    except KeyboardInterrupt:
        print("exiting...")
