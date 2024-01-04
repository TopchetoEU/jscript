package me.topchetoeu.jscript.lib;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import me.topchetoeu.jscript.interop.Arguments;
import me.topchetoeu.jscript.interop.Expose;
import me.topchetoeu.jscript.interop.ExposeConstructor;
import me.topchetoeu.jscript.interop.ExposeTarget;
import me.topchetoeu.jscript.interop.WrapperName;

@WrapperName("Date")
public class DateLib {
    private Calendar normal;
    private Calendar utc;

    private void updateUTC() {
        if (utc == null || normal == null) return;
        utc.setTimeInMillis(normal.getTimeInMillis());
    }
    private void updateNormal() {
        if (utc == null || normal == null) return;
        normal.setTimeInMillis(utc.getTimeInMillis());
    }
    private void invalidate() {
        normal = utc = null;
    }

    @Expose public double __getYear() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.YEAR) - 1900;
    }
    @Expose public double __setYeard(Arguments args) {
        var real = args.getDouble(0);
        if (real >= 0 && real <= 99) real = real + 1900;
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.YEAR, (int)real);
        updateUTC();
        return __getTime();
    }

    @Expose public double __getFullYear() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.YEAR);
    }
    @Expose public double __getMonth() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.MONTH);
    }
    @Expose public double __getDate() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.DAY_OF_MONTH);
    }
    @Expose public double __getDay() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.DAY_OF_WEEK);
    }
    @Expose public double __getHours() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.HOUR_OF_DAY);
    }
    @Expose public double __getMinutes() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.MINUTE);
    }
    @Expose public double __getSeconds() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.SECOND);
    }
    @Expose public double __getMilliseconds() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.MILLISECOND);
    }

    @Expose public double __getUTCFullYear() {
        if (utc == null) return Double.NaN;
        return utc.get(Calendar.YEAR);
    }
    @Expose public double __getUTCMonth() {
        if (utc == null) return Double.NaN;
        return utc.get(Calendar.MONTH);
    }
    @Expose public double __getUTCDate() {
        if (utc == null) return Double.NaN;
        return utc.get(Calendar.DAY_OF_MONTH);
    }
    @Expose public double __getUTCDay() {
        if (utc == null) return Double.NaN;
        return utc.get(Calendar.DAY_OF_WEEK);
    }
    @Expose public double __getUTCHours() {
        if (utc == null) return Double.NaN;
        return utc.get(Calendar.HOUR_OF_DAY);
    }
    @Expose public double __getUTCMinutes() {
        if (utc == null) return Double.NaN;
        return utc.get(Calendar.MINUTE);
    }
    @Expose public double __getUTCSeconds() {
        if (utc == null) return Double.NaN;
        return utc.get(Calendar.SECOND);
    }
    @Expose public double __getUTCMilliseconds() {
        if (utc == null) return Double.NaN;
        return utc.get(Calendar.MILLISECOND);
    }

    @Expose public double __setFullYear(Arguments args) {
        var real = args.getDouble(0);
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.YEAR, (int)real);
        updateUTC();
        return __getTime();
    }
    @Expose public double __setMonthd(Arguments args) {
        var real = args.getDouble(0);
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.MONTH, (int)real);
        updateUTC();
        return __getTime();
    }
    @Expose public double __setDated(Arguments args) {
        var real = args.getDouble(0);
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.DAY_OF_MONTH, (int)real);
        updateUTC();
        return __getTime();
    }
    @Expose public double __setDayd(Arguments args) {
        var real = args.getDouble(0);
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.DAY_OF_WEEK, (int)real);
        updateUTC();
        return __getTime();
    }
    @Expose public double __setHoursd(Arguments args) {
        var real = args.getDouble(0);
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.HOUR_OF_DAY, (int)real);
        updateUTC();
        return __getTime();
    }
    @Expose public double __setMinutesd(Arguments args) {
        var real = args.getDouble(0);
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.MINUTE, (int)real);
        updateUTC();
        return __getTime();
    }
    @Expose public double __setSecondsd(Arguments args) {
        var real = args.getDouble(0);
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.SECOND, (int)real);
        updateUTC();
        return __getTime();
    }
    @Expose public double __setMillisecondsd(Arguments args) {
        var real = args.getDouble(0);
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.MILLISECOND, (int)real);
        updateUTC();
        return __getTime();
    }

    @Expose public double __setUTCFullYeard(Arguments args) {
        var real = args.getDouble(0);
        if (Double.isNaN(real)) invalidate();
        else utc.set(Calendar.YEAR, (int)real);
        updateNormal();
        return __getTime();
    }
    @Expose public double __setUTCMonthd(Arguments args) {
        var real = args.getDouble(0);
        if (Double.isNaN(real)) invalidate();
        else utc.set(Calendar.MONTH, (int)real);
        updateNormal();
        return __getTime();
    }
    @Expose public double __setUTCDated(Arguments args) {
        var real = args.getDouble(0);
        if (Double.isNaN(real)) invalidate();
        else utc.set(Calendar.DAY_OF_MONTH, (int)real);
        updateNormal();
        return __getTime();
    }
    @Expose public double __setUTCDayd(Arguments args) {
        var real = args.getDouble(0);
        if (Double.isNaN(real)) invalidate();
        else utc.set(Calendar.DAY_OF_WEEK, (int)real);
        updateNormal();
        return __getTime();
    }
    @Expose public double __setUTCHoursd(Arguments args) {
        var real = args.getDouble(0);
        if (Double.isNaN(real)) invalidate();
        else utc.set(Calendar.HOUR_OF_DAY, (int)real);
        updateNormal();
        return __getTime();
    }
    @Expose public double __setUTCMinutesd(Arguments args) {
        var real = args.getDouble(0);
        if (Double.isNaN(real)) invalidate();
        else utc.set(Calendar.MINUTE, (int)real);
        updateNormal();
        return __getTime();
    }
    @Expose public double __setUTCSecondsd(Arguments args) {
        var real = args.getDouble(0);
        if (Double.isNaN(real)) invalidate();
        else utc.set(Calendar.SECOND, (int)real);
        updateNormal();
        return __getTime();
    }
    @Expose public double __setUTCMillisecondsd(Arguments args) {
        var real = args.getDouble(0);
        if (Double.isNaN(real)) invalidate();
        else utc.set(Calendar.MILLISECOND, (int)real);
        updateNormal();
        return __getTime();
    }

    @Expose public double __getTime() {
        if (utc == null) return Double.NaN;
        return utc.getTimeInMillis();
    }
    @Expose public double __getTimezoneOffset() {
        if (normal == null) return Double.NaN;
        return normal.getTimeZone().getRawOffset() / 60000;
    }

    @Expose public double __valueOf() {
        if (normal == null) return Double.NaN;
        else return normal.getTimeInMillis();
    }

    @Expose public String __toString() {
        return normal.getTime().toString();
    }

    @Override public String toString() {
        return __toString();
    }

    public DateLib(long timestamp) {
        normal = Calendar.getInstance();
        utc = Calendar.getInstance();
        normal.setTimeInMillis(timestamp);
        utc.setTimeZone(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(timestamp);
    }

    public DateLib() {
        this(new Date().getTime());
    }

    @ExposeConstructor public static DateLib init(Arguments args) {
        if (args.has(0)) return new DateLib(args.getLong(0));
        else return new DateLib();
    }

    @Expose(target = ExposeTarget.STATIC)
    public static double __now() {
        return new DateLib().__getTime();
    }
}
