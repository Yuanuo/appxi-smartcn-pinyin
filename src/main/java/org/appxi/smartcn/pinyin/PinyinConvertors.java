package org.appxi.smartcn.pinyin;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        final List<Map.Entry<Character, Pinyin>> pinyinList = PinyinConvertor.instance.convert(text);
        return pinyinList.stream()
                .map(entry -> null == entry.getValue()
                        ? (remainNone ? Pinyin.none5.getPinyinWithoutTone() : String.valueOf(entry.getKey()))
                        : entry.getValue().getPinyinWithoutTone())
                .collect(Collectors.joining(separator));
    }

    /**
     * 转化为拼音
     *
     * @param text 待解析的文本
     * @return 一个拼音列表
     */
    static List<Map.Entry<Character, Pinyin>> convert(String text) {
        return PinyinConvertor.instance.convert(text);
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
        final List<Map.Entry<Character, Pinyin>> pinyinList = PinyinConvertor.instance.convert(text);
        return pinyinList.stream()
                .map(entry -> String.valueOf(null == entry.getValue()
                        ? (remainNone ? Pinyin.none5.getFirstChar() : entry.getKey())
                        : entry.getValue().getFirstChar()))
                .collect(Collectors.joining(separator));
    }
}
