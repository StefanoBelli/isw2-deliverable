#!/usr/bin/python

from sys import argv
from sys import maxsize
import csv

def rowrule(c):
    # IV < FV and OV <= FV and IV <= OV
    return c[0] < c[2] and c[1] <= c[2] and c[0] <= c[1]

def cvtstr2int(r):
    ils = list()

    for c in r:
        ils.append(int(c))

    return ils

if __name__ == '__main__':
    if len(argv) == 0:
        print("missing version file path")
        exit(1)

    totalln = 0

    with open(argv[1]) as versions_f:
        reader = csv.reader(versions_f)
        oldfv = maxsize
        ln = 1
        skipped_asc_should_break = False
        for row in reader:
            if row[0] == "...":
                skipped_asc_should_break = True
                ln += 1
                continue

            comps = cvtstr2int(row)
            if oldfv > comps[2]:
                if skipped_asc_should_break:
                    print(f" --> line {ln} breaks ascending order (expected={skipped_asc_should_break})")
                    skipped_asc_should_break = False
                else:
                    print(f" --> unexpected ascending order break - terminating now, line {ln}")
                    exit(1)
            
            oldfv = comps[2]
            print(f"iv = {comps[0]}, ov = {comps[1]}, fv = {comps[2]}", end="")
            if rowrule(comps) == False:
                print(f" <-- not ok - terminating now, line {ln}")
                exit(1)
            else:
                print(" OK")
            
            ln += 1
        
        totalln = ln - 1

    print(f"graceful termination, line processed: {totalln}")
            
