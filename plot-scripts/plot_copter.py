import csv
import matplotlib.pyplot as plt

x = [];
motors = [];
alt = [];
temp = [];
press = [];

def read_csv(filename):
    with open(filename) as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        count = 0
        for row in csv_reader:
            if (count == 0):
                count += 1
            else:
                x.append(count)
                motors.append(int(row[1]))
                alt.append(int(row[2]))
                temp.append(float(row[3]))
                press.append(float(row[4]))
                count += 1

def make_plot():
#    plt.plot(x, y, 'ro', x, y)
    plt.plot(x, motors, 'r', label='motors')
    plt.plot(x, alt, 'b', label='altitude')
    plt.plot(x, temp, 'y', label='temperature')
    plt.plot(x, temp, 'y', label='temperature')
    plt.plot(x, press, 'm', label='pressure')
    plt.ylabel('value')
    plt.xlabel('sample')
    plt.title('Q7137 - 8/4 21:30')
    plt.legend(loc='upper left')
    plt.show()

if __name__ == "__main__":
    try:
        read_csv("../../log/copter/Q7137.csv")
        make_plot()
    except KeyboardInterrupt:
        print("exiting...")
