extension states -> restrictie weghalen op inkomende transities
-> check wel (zeker bij self transition) dat resulting state correct is

query order tonen bij lijntjes

transities afdwingen bij nieuwe transitie
	-> e.g. transitie A kan alleen als B en C al eens gerunt hebben (case management!)
	
	
automatic transitions in general should only be executed once by default?
rarely do you want to re-execute it should you reenter the state?

have a combo for automatic transitions
	-> once per state change (default)
	-> once per workflow
	-> unlimited
	
self transitions will always be limited to once per state change, unlimited could end in loops


allow manually setting whether a state is an end state
	-> some end states allow a revert or something
	
	
suppose state B extends A. B has no outgoing transitions itself but A does. B gets flagged as "final" workflow state it seems?
(not sure if that was the issue)