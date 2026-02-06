package tesiproject.tesi.bpmn.evaluation.resourcescost;

import java.math.BigDecimal;

public final class ResourceCostConstants {

    private ResourceCostConstants() {}

    /* kWh = CO2 * 0.00385 */
    public static final BigDecimal KWH_PER_G_CO2 = new BigDecimal("0.00333"); //Questa valutazione vale in italia

    /* costo di 1 kWh in EUR */
    public static final BigDecimal COST_EUR_PER_KWH = new BigDecimal("0.12762");

    /* normalizzazione */
    public static final BigDecimal NORMALIZATION = new BigDecimal("0.5203");
}
