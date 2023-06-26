%The Problem: Values defined in the x predicate are assigned to either 
% b or c. We want the sum of the values in b and c to be roughly the same.

%Without heuristics, this could be done using a constraint, which would
%check if the sum of values of both b and c would be close to each other (as seen below, in the verification part).
%This would, however, inevitably lead to slow and unnecessary backtracking.

%With heuristics, if we could gage the current assignment, and check the current sums of values in b and c, 
%we can prioritize assigning values to either b and c, depending on which sum is lesser.
%This would naturally create a balance between both sums. 
%If we further augment our heuristic to prioritize assigning larger values first, then the final difference
%between both sums would be minimized, and backtracking would rarely occur.

%This was only only partially possible prior to the introduction of dynamic aggregates, as normal aggregates would 
%only return the final value, and thus could not be used during the grounding-solving cycle, where heuristics apply.

%While the heuristics, if b's and c's have no further conditions to be assigned, would always follow the same pattern,
%if we introduce some independent b's or c's the heuristics can of course accommodate them.

#const maxv=20.

x(1..maxv).       %Initializing values

b(X) :- x(X), not c(X).     %Exclusive choice between c or b.
c(X) :- x(X), not b(X).




%Verification
%To verify if our set goal was achieved, we want to introduce a constraint, which would exclude answer 
%sets where the difference between the sums of values are greater than 1. For that, we need to calculate the
%final sum of values for both c and b, and compare them. In clingo the normal sum aggregate can be used.

sum_B(Sum) :- Sum = #sum{Y : b(Y)}.
sum_C(Sum) :- Sum = #sum{Y : c(Y)}.
:- sum_B(SB), sum_C(SC), (SB-SC)**2 > 1.
