elem(z,Z) :- zone2sensor(Z,D).
elem(s,D) :- zone2sensor(Z,D).

{ assign(U,T,X) } :- elem(T,X), comUnit(U).

% conversion of helper predicates needed for heuristics:
order(X,T,O) :- layer(T,X,L), O=L+1.
maxOrder(O) :- maxLayer(L), O=L+1.
maxUnit(MU) :- numUnits(MU).
maxElem(ME) :- numElems(ME).

% assign first zone in the order to the first unit.
#heuristic assign(1,T,X) : order(X,T,1). [1000]

% h1: try preceding units first.
h_assign_1(U1,T,X,W) :- order(X,T,O), assign(U,T1,Y), order(Y,T1,Om1), Om1=O-1, maxOrder(M), maxUnit(MU), comUnit(U1), U1<=U, maxElem(ME), W=10*MU*(M-O)+2*(ME-X)+U1. 
#heuristic assign(U,T,X) : h_assign_1(U,T,X,W). [W+1]

% h2: assign to an unused unit.
h_assign_2(Up1,T,X,W) :- order(X,T,O), assign(U,T1,Y), order(Y,T1,Om1), Om1=O-1, maxOrder(M), maxUnit(MU), comUnit(Up1), Up1=U+1, maxElem(ME), W=10*MU*(M-O)+2*(ME-X).
#heuristic assign(U,T,X) : h_assign_2(U,T,X,W). [W+1]

% close remaining choices:
#heuristic F assign(U,T,X) : comUnit(U), elem(T,X).

:- assign(U,T,X1), assign(U,T,X2), assign(U,T,X3), X1<X2, X2<X3.
:- assign(U1,T,X), assign(U2,T,X), U1<U2.
atLeastOneUnit(T,X):- assign(_,T,X).
:- elem(T,X), not atLeastOneUnit(T,X).

partnerunits(U,P) :- assign(U,z,Z), assign(P,s,D), zone2sensor(Z,D), U!=P.
partnerunits(U,P) :- partnerunits(P,U).
:- partnerunits(U,P1), partnerunits(U,P2), partnerunits(U,P3), P1<P2, P2<P3.

% helpers for QUICKPUP heuristics, inspired by quickpup.ascass by Erich Teppan:
sensor(S):-zone2sensor(Z,S).
zone(Z):-zone2sensor(Z,S).

numZones(N) :- zone(N), Np1=N+1, not zone(Np1).
numSensors(N) :- sensor(N), Np1=N+1, not sensor(Np1).
numElems(M):-numZones(E),numSensors(F),M=E+F.
numUnits(N) :- comUnit(N), Np1=N+1, not comUnit(Np1).

%-------topological order---
startZone(1).	% future work: other startZones (random choice?)
zoneDist(Z0,0):-startZone(Z0).

sensorDist(S,Dz1) :- zoneDist(Z,Dz),zone2sensor(Z,S),numElems(M),Dz<M, Dz1=Dz+1.
zoneDist(Z,Ds1) :- sensorDist(S,Ds),zone2sensor(Z,S),numElems(M),Ds<M, Ds1=Ds+1.

zoneLayer(Z,Dmin) :- zone(Z), zoneDist(Z,Dmin), Dminm2=Dmin-2, not zoneDist(Z,Dminm2).
sensorLayer(S,Dmin) :- sensor(S), sensorDist(S,Dmin), Dminm2=Dmin-2, not sensorDist(S,Dminm2).

layer(L) :- zoneLayer(_,L).
layer(L) :- sensorLayer(_,L).
maxLayer(N) :- layer(N), Np1=N+1, not layer(Np1).

full(U,T) :- comUnit(U), assign(U,T,X1), assign(U,T,X2), X1<X2.
assigned(T,X) :- assign(_,T,X).
used(U) :- comUnit(U), assign(U,_,_).

% translate old breadth-first syntax to new one:
layer(z,X,L) :- zoneLayer(X,L).
layer(s,X,L) :- sensorLayer(X,L).