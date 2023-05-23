%%%MANDATORY INTIAL ASSIGNMENTS

assigned_zone_unit(1,1).
assigned_sensor_unit(2,1).
assigned_sensor_unit(1,2).
assigned_zone_unit(2,2).



%%%INSTANCE SIZE 30
%assigned_zone_unit(11,1).
    %assigned_sensor_unit(20,2).
%assigned_zone_unit(21,2).


%%%INSTANCE SIZE 60
%assigned_zone_unit(21,1).
    %assigned_sensor_unit(40,2).
%assigned_zone_unit(41,2).

%%%INSTANCE SIZE 90
%assigned_zone_unit(31,1).
    %assigned_sensor_unit(60,2).
%assigned_zone_unit(61,2).

%%%INSTANCE SIZE 120
%assigned_zone_unit(41,1).
    %assigned_sensor_unit(80,2).
%assigned_zone_unit(81,2).

%%%INSTANCE SIZE 150
%assigned_zone_unit(51,1).
    %assigned_sensor_unit(100,2).
%assigned_zone_unit(101,2).

%%%INSTANCE SIZE 180
%assigned_zone_unit(61,1).
    %assigned_sensor_unit(120,2).
%assigned_zone_unit(121,2).


maxUnit(200). 
number_P(0..300).

maxZone(N) :- N = #count {Z: elem(z,Z)}.
maxSensor(N) :- N = #count {S: elem(s,S)}.

sensorNumbers(N) :- number_P(N), maxSensor(Max), N <= Max.
zoneNumbers(N) :- number_P(N), maxZone(Max), N <= Max.

degree_sensor(S,N) :- zone2sensor(Z,S), N = #count {ZN: zone2sensor(ZN,S)}. % = #count no problem 
degree_zone(Z,N) :- zone2sensor(Z,S), N = #count {SN: zone2sensor(Z,SN)}. % = #count no problem 

elem(z,Z) :- zone2sensor(Z,S).
elem(s,S) :- zone2sensor(Z,S).


% %%%%% assign zones 

assignable_zone_unit(Z,U) :-  zone2sensor(Z,S),  assigned_sensor_unit(S,U).  
assignable_zone_unit(Z,U-1) :-  zone2sensor(Z,S),  assigned_sensor_unit(S,U).
assignable_zone_unit(Z,U+1) :-  zone2sensor(Z,S),  assigned_sensor_unit(S,U).

not_assigned_zone_unit(Z,UZ) :- assigned_sensor_unit(S,US),  zone2sensor(Z,S), comUnit(UZ), (US-UZ)**2 > 1.  

not_assigned_zone_unit(Z,UZ) :- assigned_zone_unit(Z1,UZ1), zone2sensor(Z,S), zone2sensor(Z1, S), comUnit(UZ), (UZ-UZ1)**2 > 4.

not_assigned_zone_unit(Z,UZ) :- assigned_zone_unit(Z1,UZ1), zone2sensor(Z,S), zone2sensor(Z1, S), comUnit(UZ), (UZ-UZ1)**2 = 4, comUnit(UZ2), (UZ1-UZ2)**2 = 1, (UZ-UZ2)**2 = 1, assigned_sensor_unit(S1, UZ2), assigned_sensor_unit(S2, UZ2), S <> S1, S1 <> S2, S2 <> S. 

not_assigned_zone_unit(Z,U) :- assigned_zone_unit(Z1,U), assigned_zone_unit(Z2,U), Z1 <> Z2, elem(z,Z), Z <> Z1, Z <> Z2.
not_assigned_zone_unit(Z,U) :- assigned_zone_unit(Z, U1), comUnit(U), U1 <> U.

%%%%%%%%%%%%%%%
%%% choices %%%
%%%%%%%%%%%%%%%

assigned_zone_unit(Z,U)     :- assignable_zone_unit(Z,U), not not_assigned_zone_unit(Z,U),  maxUnit(MaxU), 0 < U, U < MaxU. % turn ON for heu-test
not_assigned_zone_unit(Z,U) :- assignable_zone_unit(Z,U), not assigned_zone_unit(Z,U),  maxUnit(MaxU), 0 < U, U < MaxU. % turn ON for heu-test


% %%%%% assign sensors

assignable_sensor_unit(S,U) :-  zone2sensor(Z,S),  assigned_zone_unit(Z,U).  
assignable_sensor_unit(S,U-1) :-  zone2sensor(Z,S),  assigned_zone_unit(Z,U). 
assignable_sensor_unit(S,U+1) :-  zone2sensor(Z,S),  assigned_zone_unit(Z,U). 

not_assigned_sensor_unit(S,US) :- assigned_zone_unit(Z,UZ), zone2sensor(Z,S), comUnit(US), (UZ-US)**2 > 1.

not_assigned_sensor_unit(S, US) :- assigned_sensor_unit(S1, US1), zone2sensor(Z,S), zone2sensor(Z, S1), comUnit(US), (US-US1)**2 > 4. 

not_assigned_sensor_unit(S,US) :- assigned_sensor_unit(S1,US1), zone2sensor(Z,S), zone2sensor(Z, S1), comUnit(US), (US-US1)**2 = 4, comUnit(US2), (US1-US2)**2 = 1, (US-US2)**2 = 1, assigned_zone_unit(Z1, US2), assigned_zone_unit(Z2, US2), Z <> Z1, Z1 <> Z2, Z2 <> Z. 

not_assigned_sensor_unit(S,U) :- assigned_sensor_unit(S1,U), assigned_sensor_unit(S2,U), S1 <> S2, elem(s,S), S <> S1, S <> S2.
not_assigned_sensor_unit(S, U) :- assigned_sensor_unit(S, U1), comUnit(U), U1 <> U.

%%%%%%%%%%%%%%%
%%% choices %%%
%%%%%%%%%%%%%%%

assigned_sensor_unit(S,U)     :- assignable_sensor_unit(S,U), not not_assigned_sensor_unit(S,U), maxUnit(MaxU), 0 < U, U < MaxU. % turn ON for heu-test
not_assigned_sensor_unit(S,U) :- assignable_sensor_unit(S,U), not assigned_sensor_unit(S,U),  maxUnit(MaxU), 0 < U, U < MaxU. % turn ON for heu-test



% %%%%%%%%%%%%%

assigned_zone(Z) :- assigned_zone_unit(Z,U).
assigned_sensor(S) :- assigned_sensor_unit(S,U).

:- elem(z,Z), not assigned_zone(Z). % turn ON for heu
:- elem(s,S), not assigned_sensor(S). % turn ON for heu

:- zone2sensor(Z,S), assigned_sensor_unit(S,U1), assigned_zone_unit(Z,U2), (U1-U2)**2 > 1. % turn ON for heu

sensor_blocked_on_unit(S,U) :- assignable_sensor_unit(S,U), zone2sensor(Z,S), assigned_zone_unit(Z,U1), (U1-U)**2 > 1.
sensor_blocked_on_unit(S,U) :- assignable_sensor_unit(S,U), zone2sensor(Z,S), zone2sensor(Z,SX), assigned_sensor_unit(SX,UX), (UX-U)**2 > 4.

num_sensors_on_unit(N,U) :-  comUnit(U), sensorNumbers(N), N <= #count {S1: assigned_sensor_unit(S1,U)}.
num_sensors_on_unit(2, 0).
num_zones_on_unit(N,U) 	 :-  comUnit(U), zoneNumbers(N), N <= #count {Z1: assigned_zone_unit(Z1,U)}.
num_zones_on_unit(2, 0).


num_forbidden_places_of_sensors(S, N+N1+N2) :- assignable_sensor_unit(S,_), 
	zone2sensor(Z,S), assigned_zone_unit(Z,U), 
	num_sensors_on_unit(N,U), num_sensors_on_unit(N1,U-1), num_sensors_on_unit(N2,U+1), U > 1.
	
% maybe replace with minUnit.
	
num_forbidden_places_of_sensors(S, N+2+N2) :- assignable_sensor_unit(S,_), 
	zone2sensor(Z,S), assigned_zone_unit(Z,U), 
	num_sensors_on_unit(N,U), num_sensors_on_unit(N2,U+1), U = 1.

num_forbidden_places_of_sensors(S,N+N1+2) :- assignable_sensor_unit(S,_), 
	zone2sensor(Z1,S), zone2sensor(Z2,S), 
	assigned_zone_unit(Z1,U), assigned_zone_unit(Z2,U+1), 
	num_sensors_on_unit(N,U), 
	num_sensors_on_unit(N1,U+1).

	
num_forbidden_places_of_sensors(S,N1+4) :- assignable_sensor_unit(S,_), 
	zone2sensor(Z,S), zone2sensor(Z2,S), 
	assigned_zone_unit(Z,U), assigned_zone_unit(Z2,U+2), 
	num_sensors_on_unit(N1,U+1).
													
													
													
num_forbidden_places_of_zones(Z, N+N1+N2) :- assignable_zone_unit(Z,_), 
	zone2sensor(Z,S), assigned_sensor_unit(S,U), 
	num_zones_on_unit(N,U), num_zones_on_unit(N1,U-1), num_zones_on_unit(N2,U+1), U > 1.
	
num_forbidden_places_of_zones(Z, N+2+N2) :- assignable_zone_unit(Z,_), 
	zone2sensor(Z,S), assigned_sensor_unit(S,U), 
	num_zones_on_unit(N,U), num_zones_on_unit(N2,U+1), U = 1.
	
num_forbidden_places_of_zones(Z,N+N1+2) :- assignable_zone_unit(Z,_), 
	zone2sensor(Z,S1), zone2sensor(Z,S2), 
	assigned_sensor_unit(S1,U), assigned_sensor_unit(S2,U+1), 
	num_zones_on_unit(N,U), 
	num_zones_on_unit(N1,U+1).
	
	
num_forbidden_places_of_zones(Z,N1+4) :- assignable_zone_unit(Z,_), 
	zone2sensor(Z,S), zone2sensor(Z,S2), 
	assigned_sensor_unit(S,U), assigned_sensor_unit(S2,U+2), 
	num_zones_on_unit(N1,U+1).

#heuristic assigned_sensor_unit(S,U) : 
        assignable_sensor_unit(S,U),
        not not_assigned_sensor_unit(S,U),
        not sensor_blocked_on_unit(S,U),
        
        NAZ = #count {N : assigned_zone_unit(N, _)},
        
        NAS = #count {M : assigned_sensor_unit(M, _)},
        
        maxSensor(MaxS), 
        NAS < MaxS,
        NAZ > NAS,
        
        Deg_sensor_dyn = #count {ZN: zone2sensor(ZN,S), assigned_zone_unit(ZN, _)},
	
        Forbidden_placement_total = #max {N: num_forbidden_places_of_sensors(S, N)},
	
        
        Num_constr_on_sensor_places_by_zones_on_U1     = 	#count {SN : assigned_zone_unit(Z,U1), zone2sensor(Z,SN)}, 
        Num_constr_on_sensor_places_by_zones_on_U	 = 	#count {SN : assigned_zone_unit(Z,U),   zone2sensor(Z,SN)}, 
        Num_constr_on_sensor_places_by_zones_on_U2	 =	#count {SN : assigned_zone_unit(Z,U2), zone2sensor(Z,SN)},
        Num_sat_constr_on_sensor_places_by_zones_on_U1  = 	#count {SN : assigned_zone_unit(Z,U1), zone2sensor(Z,SN),  assigned_sensor(SN)}, 
        Num_sat_constr_on_sensor_places_by_zones_on_U	 = 	#count {SN : assigned_zone_unit(Z,U),   zone2sensor(Z,SN),  assigned_sensor(SN)}, 
        Num_sat_constr_on_sensor_places_by_zones_on_U2	 =	#count {SN : assigned_zone_unit(Z,U2), zone2sensor(Z,SN),  assigned_sensor(SN)},
	

        Open_constraints_on_placement = Num_constr_on_sensor_places_by_zones_on_U1
                                        + Num_constr_on_sensor_places_by_zones_on_U
                                        + Num_constr_on_sensor_places_by_zones_on_U2 
                                        - Num_sat_constr_on_sensor_places_by_zones_on_U1
                                        - Num_sat_constr_on_sensor_places_by_zones_on_U
                                        - Num_sat_constr_on_sensor_places_by_zones_on_U2,
        comUnit(U),
        degree_sensor(S, Deg_sensor_stat),
        Minus_deg_sensor_stat = 6 - Deg_sensor_stat,
        
        
        W = Deg_sensor_dyn * 1000 + Forbidden_placement_total * 100 + Open_constraints_on_placement * 10 + Minus_deg_sensor_stat + 0.  [W@0]
        
    
        
    #heuristic assigned_zone_unit(Z,U) : 
        assignable_zone_unit(Z,U),
        not not_assigned_zone_unit(Z,U),
        
        Forbidden_placement_total = #max {FN2: num_forbidden_places_of_zones(Z, FN2)},
        Assigned_zones_unit = #count{ZN: assigned_zone_unit(ZN,U)},
        
        Min_constraint_degree = #min{DN: zone2sensor(Z, SN), degree_sensor(SN, DN)},
        
        Minus_min_constraint_degree = 6 - Min_constraint_degree,
        
        NAZ = #count {M1 : assigned_zone_unit(M1, _)},
        
        NAS = #count {M2 : assigned_sensor_unit(M2, _)},
        
        maxZone(MaxZ), 
        NAZ < MaxZ,
        NAZ <= NAS,
                                        
        Direct_con_sensors = #count {S2 : assigned_sensor_unit(S2,U), zone2sensor(Z,S2)},
        
        comUnit(U),
        
        W = Assigned_zones_unit * 1000 + Forbidden_placement_total * 100 + Minus_min_constraint_degree * 10 + Direct_con_sensors + 0.  [W@0]
	


