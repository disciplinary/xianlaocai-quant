package xlc.quant.data.indicator.calculator;

import java.util.function.Consumer;
import java.util.function.Function;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import xlc.quant.data.indicator.Indicator;
import xlc.quant.data.indicator.IndicatorCalculator;
import xlc.quant.data.indicator.IndicatorComputeCarrier;
import xlc.quant.data.indicator.util.DoubleUtils;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DMI extends Indicator {

	/** 又名DI1或者+DI，上升方向线  */
	private Double dip;
	/** 又名DI2或者-DI，下降方向线 */
	private Double dim;
	
	/** 动向平均数ADX，趋向平均线 */
	private Double adx;
	
	/** 评估数值ADXR，趋向评估线 */
	private Double adxr;

	//辅助计算数字
	/** 真实波动范围（True Range） */
	private Double tr;

	/** 上升动向（+DM）dmPlus */
	private Double dmp;

	/** 下降动向（-DM）dmMinus */
	private Double dmm;

	/** 动向指数DX */
	private Double dx;

	/**
	 * @param tr  真实波动范围（True Range）
	 * @param dmp 上升动向（+DM）dmPlus
	 * @param dmm 下降动向（-DM）dmMinus
	 */
	public DMI(Double tr, Double dmp, Double dmm) {
		super();
		this.tr = tr;
		this.dmp = dmp;
		this.dmm = dmm;
	}

	//=============
	//内部类分隔符 XXX
	//=============
	/**
	 * 构建-计算器
	 * @param diPeriod
	 * @param adxPeriod
	 * @return
	 */
	public static <C extends IndicatorComputeCarrier<?>> IndicatorCalculator<C, DMI> buildCalculator(int diPeriod, int adxPeriod) {
		return new DMICalculator<>(diPeriod, adxPeriod);
	}
	

	/**
	 * 内部类实现DMI计算器
	 * @author Rootfive
	 */
	private static class DMICalculator<C extends IndicatorComputeCarrier<?>> extends IndicatorCalculator<C, DMI> {
		
		/** 趋向周期 */
		private final int adxPeriod;

		/**
		 * @param diPeriod 动向周期
		 * @param adxPeriod 趋向周期
		 */
		DMICalculator(int diPeriod, int adxPeriod) {
			super(diPeriod, false);
			this.adxPeriod = adxPeriod;
		}

		
		/**
		 *  百度百科：https://baike.baidu.com/item/DMI%E6%8C%87%E6%A0%87/3423254#4
		 */
		@Override
		protected DMI executeCalculate(Function<C, DMI> propertyGetter,Consumer<DMI> propertySetter) {
			C prev = getPrev();
			C head = getHead();

			Double tr = null;
			Double dmp = null;
			Double dmm = null;
			if (prev == null) {
				tr = head.getHigh()-head.getLow();
				dmp = DoubleUtils.ZERO;
				dmm = DoubleUtils.ZERO;
			} else {
				Double highLowDiff = head.getHigh()- head.getLow();
				Double highPrevCloseDiff = head.getHigh()- head.getPreClose();
				Double lowPrevCloseDiff = head.getLow()-head.getPreClose();
				tr = DoubleUtils.max(highLowDiff,Math.abs(highPrevCloseDiff),Math.abs(lowPrevCloseDiff));

				Double highMinusHighPrev = head.getHigh()-prev.getHigh();
				dmp = Math.max(highMinusHighPrev, DoubleUtils.ZERO);

				Double LowPrevMinusLow = prev.getLow()-head.getLow();
				dmm = Math.max(LowPrevMinusLow, DoubleUtils.ZERO);

				int compareTo = dmp.compareTo(dmm);
				if (compareTo > 0) {
					dmm = DoubleUtils.ZERO;
				} else if (dmp.compareTo(dmm) < 0) {
					dmp = DoubleUtils.ZERO;
				}
			}

			DMI dmiHead = new DMI(tr, dmp, dmm);
			
			double trSum = dmiHead.getTr();
			double dmpSum = dmiHead.getDmp();
			double dmmSum = dmiHead.getDmm();
			
			for (int i = 1; i < circularData.length; i++) {
				DMI dmi_i = propertyGetter.apply(getPrevByNum(i));
				
				trSum= trSum+dmi_i.getTr();
				dmpSum= dmpSum+dmi_i.getDmp();
				dmmSum= dmmSum+dmi_i.getDmm();
			}

			Double dip = null;
			Double dim = null;
			if (trSum == 0) {
				//连续横盘的极端情况
				dip = DoubleUtils.ZERO;
				dim = DoubleUtils.ZERO;
			}else {
				dip = DoubleUtils.divideByPct(dmpSum, trSum);
				dim = DoubleUtils.divideByPct(dmmSum, trSum);
			}
			dmiHead.setDip(dip);
			dmiHead.setDim(dim);

			//依据DI值可以计算出DX指标值。其计算方法是将+DI和—DI间的差的绝对值除以总和的百分比得到动向指数DX
			Double diDiff = Math.abs(dip-dim);
			Double diSum = dip+dim;
			double dx = DoubleUtils.ZERO;
			if (diSum > DoubleUtils.ZERO) {
				dx = DoubleUtils.divideByPct(diDiff, diSum);
			}
			
			dmiHead.setDx(dx);

			if (executeTotal <= adxPeriod) {
				//设置计算结果
				propertySetter.accept(dmiHead);
				return dmiHead;
			}
			
			double dxSum =  dmiHead.getDx();
			for (int i = 1; i < adxPeriod; i++) {
				DMI dmi_i = propertyGetter.apply(getPrevByNum(i));
				dxSum = dxSum+dmi_i.getDx();
			}
			
			Double adx = DoubleUtils.divide(dxSum, adxPeriod, 2);
			dmiHead.setAdx(adx);

			Double adxr = null;
			Double adxByPrevAdxPeriod = propertyGetter.apply(getPrevByNum(adxPeriod)).getAdx();
			if (adxByPrevAdxPeriod != null) {
				adxr = DoubleUtils.divide(adx+adxByPrevAdxPeriod, 2, 2);
			}
			
			dmiHead.setAdxr(adxr);
			//设置计算结果
			propertySetter.accept(dmiHead);
			return dmiHead;
		}
	}

}
