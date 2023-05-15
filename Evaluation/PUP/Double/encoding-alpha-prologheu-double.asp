assigned_zone_unit(1, 1).

maxUnit(80). 
startUnit(15). 
number_P(0..100).

basis(10).										
								
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
        T assignable_sensor_unit(S,U), 
        not T not_assigned_sensor_unit(S,U),
        not T sensor_blocked_on_unit(S,U),        
        
        Deg_sensor_dyn = #count {ZN: zone2sensor(ZN,S), assigned_zone_unit(ZN, _)},
	
        Forbidden_placement_total = #max {N: num_forbidden_places_of_sensors(S, N)},
	
        Assigned_sensors_unit = #count{SN: assigned_sensor_unit(SN,U)},

        
        Direct_con_zones = #count {Z : assigned_zone_unit(Z,U), zone2sensor(Z,S) }, 
        
        
        
        
        W = Deg_sensor_dyn * 10000 + Forbidden_placement_total * 1000 + Assigned_sensors_unit * 100 + Direct_con_zones * 10.  [W@0]
  
%Adjacent_sensors = #count {S1 : zone2sensor(Z, S), zone2sensor(Z, S1), S1 != S},
        
%Adjacent_sensors_negative = 5 - Adjacent_sensors,

% Adjacent_sensors_negative + 0
    
        
#heuristic assigned_zone_unit(Z,U) : 
        T assignable_zone_unit(Z,U), 
        not T not_assigned_zone_unit(Z,U),
        
        
        Assigned_zones_unit = #count{ZN2: assigned_zone_unit(ZN2,U)},
        
        U1 = U - 1,
        U2 = U + 1,
        
        N0 = #max{N : num_sensors_on_unit(N, U)},
        
        N1 = #max{N : num_sensors_on_unit(N, U1)},
        N2 = #max{N : num_sensors_on_unit(N, U2)},
        
        Available_spaces_for_sensors = 6 - (N0 + N1 + N2), 


        
        W = Assigned_zones_unit * 10 + Available_spaces_for_sensors.  [W@1]
	


