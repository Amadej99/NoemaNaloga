## Disclaimer

Program je neobčutljiv na velike / male črke prav tako na presledke. Če naprava nima dostopa do interneta na to opozori, opozori tudi če vnesemo neveljavno kodo države ali pa nasploh neveljavni vhod.

Za izpis šumnikov na standardni izhod se zanaša na podporo UTF-8 encodinga na napravi.

## Primer uporabe

```console
javac VATChecker.java
java VATChecker 
Vnesi davcno stevilko v formatu SI XXXXXXXX: SI 98511734
--------------------------------------------------------------
TELEKOM SLOVENIJE, D.D.
CIGALETOVA ULICA 015, LJUBLJANA, 1000 LJUBLJANA
--------------------------------------------------------------
```

```console
javac VATChecker.java
java VATChecker 
Vnesi davcno stevilko v formatu SI XXXXXXXX: si48944882
--------------------------------------------------------------
NOEMA COOPERATING D.O.O.
ŽELEZNA CESTA 014, LJUBLJANA, 1000 LJUBLJANA
--------------------------------------------------------------
```

```console
javac VATChecker.java
java VATChecker 
Vnesi davcno stevilko v formatu SI XXXXXXXX: SI amadej
Vnesi veljavno davcno stevilko!
Vnesi davcno stevilko v formatu SI XXXXXXXX: SI95606599
--------------------------------------------------------------
MSC KOPER D.O.O.
ULICA 15.MAJA 2 B, 6000 KOPER - CAPODISTRIA
--------------------------------------------------------------
```