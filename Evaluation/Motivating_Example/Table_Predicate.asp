table(N, B_Step_Sum, Diff_B, C_Step_Sum, Diff_C) :- x(N), step_Sum_B(B_Step_Sum, N), step_Sum_C(C_Step_Sum, N), sum_B(B_Sum), Diff_B = B_Sum - B_Step_Sum, sum_C(C_Sum), Diff_C = C_Sum - C_Step_Sum. 
