Core features supported:
- Custom arbitrary-precision integer arithmetic
- Exact rational arithmetic
- Exact radicals and fractional powers
- Symbolic simplification of radical sums
- Exact reciprocal/rationalization using a generated radical basis and rational linear systems
- Parser for calculator-style input
- Basic matrix/vector support

These are some custom expressions and results that can be output by the program
- Integer addition: 194619734936439264826483618271043827414814 + 8294294719472946194946710847308712097483 = 202914029655912211021430329118352539512297
- Fraction parsing: 0.187187187187{187187} = 187/999 and 0.1{6} = 1/6
- Exponent simplification: 64^3.1{6} = 524288 and 1125899906842624^1.4 = 1180591620717411303424
- Reciprocation over a basis: 1 / (1 + 2^(4/17)) = 1/17 - 8/17*(2)^(1/17) - 4/17*(2)^(2/17) - 2/17*(2)^(3/17) - 1/17*(2)^(4/17) + 8/17*(2)^(5/17) + 4/17*(2)^(6/17) + 2/17*(2)^(7/17) + 1/17*(2)^(8/17) - 8/17*(2)^(9/17) - 4/17*(2)^(10/17) - 2/17*(2)^(11/17) - 1/17*(2)^(12/17) + 8/17*(2)^(13/17) + 4/17*(2)^(14/17) + 2/17*  (2)^(15/17) + 1/17*(2)^(16/17)
- RREF with exact fractions(which was used to generate the above)
- Base-10 to arbitrary integer base ≥ 2
- A few basic things with statistics, like mean, median, standard deviation, etc.
