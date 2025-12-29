/*
 * This file is part of the zyan/wework-msgaudit.
 *
 * (c) 读心印 <aa24615@qq.com>
 *
 * This source file is subject to the MIT license that is bundled
 * with this source code in the file LICENSE.
 */


package com.ruoran.houyi;

import it.sauronsoftware.jave.*;

import java.io.File;

/**
 * @author renlu
 */
public class Audio {

    public static void toMp3(String sourcePath, String targetPath) {

        File source = new File(sourcePath);
        File target = new File(targetPath);
        AudioUtils.amrToMp3(source, target);
    }
}
