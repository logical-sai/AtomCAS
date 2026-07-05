package com.AtomCAS.utilities;
public class RadicalExpression {
    public LargeMap<LargePowerNode, LargeFraction> sum;
    public LargeFraction coefficient;
    public RadicalExpression(){
        sum = new LargeMap<>();
        coefficient = LargeFraction.zero;
    }
    public RadicalExpression(LargeMap<LargePowerNode, LargeFraction> sum, LargeFraction coefficient){
        this.sum = sum;
        this.coefficient = coefficient;
    }
}
