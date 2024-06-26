#!/usr/bin/python3
import csv
import sys
with open(f"{sys.argv[1]}-Result.csv") as csvf:
	reader = csv.reader(csvf)
	for row in reader:
		if str.lower(row[5]) == str.lower(sys.argv[2]) and \
			str.lower(row[6]) == str.lower(sys.argv[3]) and \
			str.lower(row[7]) == str.lower(sys.argv[4]) and \
			str.lower(row[8]) == str.lower(sys.argv[5]) and \
			str(int(row[1])) == sys.argv[6]:
			print(f"{row[17]}")
			break
