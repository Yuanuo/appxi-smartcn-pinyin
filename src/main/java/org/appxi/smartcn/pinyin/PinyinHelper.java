/*
 * <summary></summary>
 * <author>He Han</author>
 * <email>hankcs.cn@gmail.com</email>
 * <create-date>2014/11/2 11:19</create-date>
 *
 * <copyright file="PinyinHelper.java" company="上海林原信息科技有限公司">
 * Copyright (c) 2003-2014, 上海林原信息科技有限公司. All Right Reserved, http://www.linrunsoft.com/
 * This source is subject to the LinrunSpace License. Please contact 上海林原信息科技有限公司 to get more information.
 * </copyright>
 */
package org.appxi.smartcn.pinyin;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hankcs
 */
class PinyinHelper {
    /**
     * Convert tone numbers to tone marks using Unicode <br/><br/>
     * <p/>
     * <b>Algorithm for determining location of tone mark</b><br/>
     * <p/>
     * A simple algorithm for determining the vowel on which the tone mark
     * appears is as follows:<br/>
     * <p/>
     * <ol>
     * <li>First, look for an "a" or an "e". If either vowel appears, it takes
     * the tone mark. There are no possible pinyin syllables that contain both
     * an "a" and an "e".
     * <p/>
     * <li>If there is no "a" or "e", look for an "ou". If "ou" appears, then
     * the "o" takes the tone mark.
     * <p/>
     * <li>If none of the above cases hold, then the last vowel in the syllable
     * takes the tone mark.
     * <p/>
     * </ol>
     *
     * @param pinyinStr the ascii represention with tone numbers
     * @return the unicode represention with tone marks
     */
    public static String convertToneNumber2ToneMark(String pinyinStr) {
        pinyinStr = pinyinStr.toLowerCase();

        if (!pinyinStr.matches("[a-z]*[1-5]?")) {
            return pinyinStr;
        }
        // bad format

        final char defautlCharValue = '$';
        final int defautlIndexValue = -1;

        char unmarkedVowel = defautlCharValue;
        int indexOfUnmarkedVowel = defautlIndexValue;

        final char charA = 'a';
        final char charE = 'e';
        final String ouStr = "ou";
        final String allUnmarkedVowelStr = "aeiouv";
        final String allMarkedVowelStr = "āáǎàaēéěèeīíǐìiōóǒòoūúǔùuǖǘǚǜü";

        if (!pinyinStr.matches("[a-z]*[1-5]")) {
            // only replace v with ü (umlat) character
            return pinyinStr.replaceAll("v", "ü");
        }
        // input string has no any tune number

        int tuneNumber =
                Character.getNumericValue(pinyinStr.charAt(pinyinStr.length() - 1));

        int indexOfA = pinyinStr.indexOf(charA);
        int indexOfE = pinyinStr.indexOf(charE);
        int ouIndex = pinyinStr.indexOf(ouStr);

        if (-1 != indexOfA) {
            indexOfUnmarkedVowel = indexOfA;
            unmarkedVowel = charA;
        } else if (-1 != indexOfE) {
            indexOfUnmarkedVowel = indexOfE;
            unmarkedVowel = charE;
        } else if (-1 != ouIndex) {
            indexOfUnmarkedVowel = ouIndex;
            unmarkedVowel = ouStr.charAt(0);
        } else {
            for (int i = pinyinStr.length() - 1; i >= 0; i--) {
                if (String.valueOf(pinyinStr.charAt(i)).matches(
                        "[" + allUnmarkedVowelStr + "]")) {
                    indexOfUnmarkedVowel = i;
                    unmarkedVowel = pinyinStr.charAt(i);
                    break;
                }
            }
        }

        if ((defautlCharValue != unmarkedVowel) && (defautlIndexValue != indexOfUnmarkedVowel)) {
            int rowIndex = allUnmarkedVowelStr.indexOf(unmarkedVowel);
            int columnIndex = tuneNumber - 1;
            int vowelLocation = rowIndex * 5 + columnIndex;
            char markedVowel = allMarkedVowelStr.charAt(vowelLocation);

            final StringBuilder result = new StringBuilder();
            result.append(pinyinStr.substring(0, indexOfUnmarkedVowel).replaceAll("v",
                    "ü"));
            result.append(markedVowel);
            result.append(pinyinStr.substring(indexOfUnmarkedVowel + 1,
                    pinyinStr.length() - 1).replaceAll("v", "ü"));
            return result.toString();
        } else
        // error happens in the procedure of locating vowel
        {
            return pinyinStr;
        }
    }

    /**
     * 将列表转为数组
     *
     * @param pinyinList
     * @return
     */
    public static Pinyin[] convertList2Array(List<Pinyin> pinyinList) {
        return pinyinList.toArray(new Pinyin[0]);
    }

    public static Pinyin removeTone(Pinyin p) {
        return Pinyin.none5;
    }

    /**
     * 转换List<Pinyin> pinyinList到List<String>，其中的String为带声调符号形式
     *
     * @param pinyinList
     * @return
     */
    public static List<String> convertPinyinList2TonePinyinList(List<Pinyin> pinyinList) {
        final List<String> result = new ArrayList<>(pinyinList.size());
        for (Pinyin pinyin : pinyinList) {
            result.add(pinyin.getPinyinWithToneMark());
        }
        return result;
    }

    private PinyinHelper() {
    }
}
