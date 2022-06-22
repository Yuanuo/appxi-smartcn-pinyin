package org.appxi.smartcn.pinyin;

import org.appxi.smartcn.util.SmartCNHelper;
import org.appxi.smartcn.util.bytes.ByteArray;
import org.appxi.smartcn.util.bytes.BytesHelper;
import org.appxi.smartcn.util.dictionary.StringDictionary;
import org.appxi.smartcn.util.trie.AbstractDictionaryTrieApp;
import org.appxi.smartcn.util.trie.DoubleArrayTrieByAhoCorasick;
import org.appxi.util.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PinyinConvertor extends AbstractDictionaryTrieApp<Pinyin[]> {
    private static final Logger logger = LoggerFactory.getLogger(PinyinConvertor.class);

    public static final PinyinConvertor ONE = new PinyinConvertor();

    private PinyinConvertor() {
        // 删除旧版数据
        FileHelper.deleteDirectory(SmartCNHelper.resolveData("pinyin"));
        FileHelper.deleteDirectory(SmartCNHelper.resolveCache("pinyin"));
    }

    @Override
    protected final void loadDictionaries(DoubleArrayTrieByAhoCorasick<Pinyin[]> trie) {
        // default
        URLConnection txtFileDefault = null;
        try {
            txtFileDefault = getClass().getResource("data.txt").openConnection();
        } catch (Exception e) {
            logger.warn("should never here", e);
        }
        // user managed
        final Path txtFileManaged = SmartCNHelper.resolveData("pinyin.txt");
        // cache file
        final Path binFile = SmartCNHelper.resolveCache("pinyin.bin");
        // 检查缓存bin文件是否需要重建
        if (!FileHelper.isTargetFileUpdatable(binFile, txtFileDefault, txtFileManaged)) {
            // load from bin
            final long st = System.currentTimeMillis();
            try {
                final ByteArray byteArray = BytesHelper.createByteArray(binFile);
                if (null != byteArray) {
                    final Pinyin[] valueEnums = Pinyin.values();
                    final int totalSize = byteArray.nextInt();
                    final Pinyin[][] valueArray = new Pinyin[totalSize][];
                    for (int i = 0; i < valueArray.length; ++i) {
                        final int itemSize = byteArray.nextInt();
                        valueArray[i] = new Pinyin[itemSize];
                        for (int j = 0; j < itemSize; ++j) {
                            valueArray[i][j] = valueEnums[byteArray.nextInt()];
                        }
                    }
                    trie.load(byteArray, valueArray);
                    return;
                }
            } finally {
                logger.info("loadBin used time: " + (System.currentTimeMillis() - st));
            }
        }
        // load primary txt
        final TreeMap<String, Pinyin[]> primaryMap = new TreeMap<>();
        // 加载默认数据
        _load(primaryMap, txtFileDefault);
        // 加载管理的数据，可以覆盖默认数据
        _load(primaryMap, txtFileManaged);

        // build to trie
        long st = System.currentTimeMillis();
        trie.build(primaryMap);
        logger.info("trie.build + " + (System.currentTimeMillis() - st));
        // save to bin
        FileHelper.makeParents(binFile);
        st = System.currentTimeMillis();
        if (trie.size() == primaryMap.size()) {
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(binFile)))) {
                out.writeInt(primaryMap.size());
                Pinyin[] value;
                for (Map.Entry<String, Pinyin[]> entry : primaryMap.entrySet()) {
                    value = entry.getValue();
                    out.writeInt(value.length);
                    for (Pinyin pinyin : value) {
                        out.writeInt(pinyin.ordinal());
                    }
                }
                trie.save(out);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        logger.info("saveBin used time: " + (System.currentTimeMillis() - st));
    }

    private void _load(TreeMap<String, Pinyin[]> primaryMap, Object source) {
        if (null == source) {
            logger.warn("source is null");
            return;
        }

        final StringDictionary dictionary = new StringDictionary("=");
        //
        String sourcePath = null;
        if (source instanceof Path path && FileHelper.exists(path)) {
            sourcePath = path.toString();
            try (InputStream stream = Files.newInputStream(path)) {
                dictionary.load(stream);
            } catch (IOException e) {
                logger.warn("load Path failed", e);
            }
        } else if (source instanceof URLConnection urlConn) {
            sourcePath = urlConn.getURL().toString();
            try (InputStream stream = new BufferedInputStream(urlConn.getInputStream())) {
                dictionary.load(stream);
            } catch (IOException e) {
                logger.warn("load URL failed", e);
            }
        }
        //
        final String finalSourcePath = sourcePath;
        dictionary.walkEntries((k, v) -> {
            try {
                final Object[] tmpArr = v.split(",");
                final Pinyin[] valArr = new Pinyin[tmpArr.length];
                for (int i = 0; i < tmpArr.length; ++i) {
                    valArr[i] = Pinyin.valueOf((String) tmpArr[i]);
                }
                primaryMap.put(k, valArr);
            } catch (Exception e) {
                logger.warn("拼音词典" + finalSourcePath + "有问题在【" + k + "=" + v + "】", e);
            }
        });
    }

    public final List<Map.Entry<Character, Pinyin>> convert(String string) {
        return convert(string.toCharArray());
    }

    public final List<Map.Entry<Character, Pinyin>> convert(char... chars) {
        final Pinyin[][] wordNet = new Pinyin[chars.length][];
        getDictionaryTrie().parseText(chars, (begin, end, value) -> {
            int length = end - begin;
            if (wordNet[begin] == null || length > wordNet[begin].length) {
                wordNet[begin] = length == 1 ? new Pinyin[]{value[0]} : value;
            }
        });
        //
        final List<Map.Entry<Character, Pinyin>> result = new ArrayList<>(chars.length);
        for (int i = 0; i < wordNet.length; ) {
            if (wordNet[i] == null) {
                result.add(new AbstractMap.SimpleEntry<>(chars[i++], null));
                continue;
            }
            for (Pinyin itm : wordNet[i]) {
                result.add(new AbstractMap.SimpleEntry<>(chars[i++], itm));
            }
        }
        return result;
    }
}
