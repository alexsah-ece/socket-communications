import csv
import matplotlib.pyplot as plt

x = [];
y = [];
z = [];

def read_csv(filename):
    with open(filename) as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        count = 0
        for row in csv_reader:
            if (count == 0):
                count += 1
            else:
                x.append(int(row[0]))
                y.append(int(row[1]))

def make_plot():
#    plt.plot(x, y, 'ro', x, y)
    plt.plot(x, y)
    plt.ylabel('mean')
    plt.xlabel('packet')
    plt.title('A8633AQF - 8/4 21:30')
    plt.show()

if __name__ == "__main__":
    try:
        read_csv("../../log/AQDPCM/A8633AQF_means.csv")
        make_plot()
    except KeyboardInterrupt:
        print("exiting...")
