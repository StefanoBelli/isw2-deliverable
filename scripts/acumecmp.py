#!/usr/bin/python3

from sys import argv
import csv

result_filename = {
    'APACHE STORM': '../csv_output/Apache Storm-Result.csv',
    'BOOKKEEPER': '../csv_output/Bookkeeper-Result.csv'
}

epsilon = 0.0001

def findcsvcolidx(row, igncasecolname="npofb20"):
    i = 0
    for col in row:
        if str.lower(col) == igncasecolname:
            return i
        i += 1

if __name__ == '__main__':
    if len(argv) == 1:
        print("specify ACUME output file (\"EAM_NEAM_output.csv\")")
        exit(1)
    elif len(argv) >= 3:
        epsilon = float(argv[2])
    
    with open(argv[1]) as acume_f:
        acume_f_reader = csv.reader(acume_f)
        is_header = True
        npofb20_acume_col_idx = 0
        for acume_row in acume_f_reader:
            if is_header == True:
                is_header = False
                npofb20_acume_col_idx = findcsvcolidx(acume_row)
                continue

            if len(acume_row) == 0:
                continue

            acume_input_filename = acume_row[0]
            predcomp = acume_input_filename.split('_')
            acume_npofb20 = float(acume_row[npofb20_acume_col_idx])
            result_npofb20 = None
            found = False
            with open(result_filename[predcomp[0]]) as result_f:
                result_f_reader = csv.reader(result_f)
                is_result_header = True
                npofb20_result_col_idx = 0
                for result_row in result_f_reader:
                    if is_result_header:
                        npofb20_result_col_idx = findcsvcolidx(result_row)
                        is_result_header = False
                        continue

                    if str.lower(result_row[5]) == str.lower(predcomp[1]) and \
                        str.lower(result_row[6]) == str.lower(predcomp[2]) and \
                        str.lower(result_row[7]) == str.lower(predcomp[3]) and \
                        str.lower(result_row[8]) == str.lower(predcomp[4]) and \
                        result_row[1] == predcomp[5][:- 4]:
                    
                        result_npofb20 = float(result_row[npofb20_result_col_idx])

                        print(f"{predcomp[0]}, {predcomp[1]}, {predcomp[2]}, {predcomp[3]}, {predcomp[4]}, {predcomp[5]}")
                        found = True
                        break

            if found == False:
                print("not found ???? exiting now...")
                exit(2)

            print(f"\tacume = {acume_npofb20}, result = {result_npofb20}", end="")

            if abs(result_npofb20 - acume_npofb20) > epsilon:
                print(f" <-- DIFF (by {abs(result_npofb20 - acume_npofb20)})")
            else:
                print(" OK")

    
    print()
    print(f"graceful termination, epsilon = {epsilon}")
    print()