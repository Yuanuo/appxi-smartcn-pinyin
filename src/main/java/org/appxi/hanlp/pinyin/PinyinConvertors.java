package org.appxi.hanlp.pinyin;

import java.util.List;

public interface PinyinConvertors {
    /**
     * 转化为拼音
     *
     * @param text       文本
     * @param separator  分隔符
     * @param remainNone 有些字没有拼音（如标点），是否保留它们的拼音（true用none表示，false用原字符表示）
     * @return 一个字符串，由[拼音][分隔符][拼音]构成
     */
    static String convert(String text, String separator, boolean remainNone) {
        final List<Pinyin> pinyinList = PinyinConvertor.instance.convert(text, true);
        final int length = pinyinList.size();
        final StringBuilder result = new StringBuilder(length * (5 + separator.length()));
        for (int i = 0; i < text.length(); i++) {
            final Pinyin pinyin = pinyinList.get(i);
            if (pinyin == Pinyin.none5 && !remainNone) result.append(text.charAt(i));
            else result.append(pinyin.getPinyinWithoutTone());
            if (i < length) result.append(separator);
        }
        return result.toString();
    }

    /**
     * 转化为拼音
     *
     * @param text 待解析的文本
     * @return 一个拼音列表
     */
    static List<Pinyin> convert(String text) {
        return PinyinConvertor.instance.convert(text, true);
    }

    /**
     * 转化为拼音（首字母）
     *
     * @param text       文本
     * @param separator  分隔符
     * @param remainNone 有些字没有拼音（如标点），是否保留它们（用none表示）
     * @return 一个字符串，由[首字母][分隔符][首字母]构成
     */
    static String convertToFirstChars(String text, String separator, boolean remainNone) {
        final List<Pinyin> pinyinList = PinyinConvertor.instance.convert(text, remainNone);
        final int length = pinyinList.size();
        final StringBuilder result = new StringBuilder(length * (1 + separator.length()));
        for (Pinyin pinyin : pinyinList) {
            if (result.length() > 0) result.append(separator);
            result.append(pinyin.getFirstChar());
        }
        return result.toString();
    }
}
