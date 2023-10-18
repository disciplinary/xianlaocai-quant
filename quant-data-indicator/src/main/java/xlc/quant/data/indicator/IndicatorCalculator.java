package xlc.quant.data.indicator;

/**
 * 指标计算 计算器
 * 
 * @author Rootfive
 * 
 */
public abstract  class IndicatorCalculator<C extends IndicatorComputeCarrier<?>,I>  extends  CircularFixedWindowCalculator<C,I> {

	public IndicatorCalculator(int period, boolean isFullCapacityCalculate) {
		super(period,  isFullCapacityCalculate);
	}

	// ==========XXX===================

	/**
	 * @param current  当前值
	 * @param prev	   前值	
	 * @param preContinueValue  前连续值
	 * @return
	 */
	public static int getContinueValue(Double current, Double prev, Integer preContinueValue) {
		if (current == null || prev == null) {
			return 0;
		}
		
		if (preContinueValue == null) {
			preContinueValue = 0;
		}
		
		int compareResult = current.compareTo(prev);
		switch (compareResult) {
		case 1:
			//1,current [>] prev
			if (preContinueValue > 0) {
				//前值 > 0
				return preContinueValue + 1;
			} else if (preContinueValue == 0) {
				//前值 = 0
				return 1;
			} else {
				//前值 < 0
				return 1;
			}
		case 0:
			//0,current [=] prev
			return 0;
		default:
			//-1,current [<] prev
			if (preContinueValue > 0) {
				//前值 > 0
				return -1;
			} else if (preContinueValue == 0) {
				//前值 = 0
				return -1;
			} else {
				//前值 < 0
				return preContinueValue - 1;
			}
		}
	}
}