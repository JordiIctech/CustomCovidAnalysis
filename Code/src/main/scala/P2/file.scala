package P2

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.io.IOUtils
import org.apache.spark.sql.DataFrame


object file {

  def outputJson(name : String,newData:DataFrame): Unit =  {

    val outputfile = "C:\\outputJson"
    var filename = name + ".json"
    var outputFileName = outputfile + "/temp_" + filename
    var mergedFileName = outputfile + "/" + filename//merged_
    var mergeFindGlob  = outputFileName
    var fileDel = outputfile + "/." + filename + ".crc"

    newData.write
      .format("org.apache.spark.sql.json")
      .option("header", "true")
      .mode("overwrite")
      .save(outputFileName)
    merge(mergeFindGlob, mergedFileName,fileDel)
    newData.unpersist()
  }

  def outputcsv(name : String,newData:DataFrame): Unit =  {

    val outputfile = "C:\\outputcsv"
    var filename = name + ".csv"
    var outputFileName = outputfile + "/temp_" + filename
    var mergedFileName = outputfile + "/" + filename//merged_
    var mergeFindGlob  = outputFileName
    var fileDel = outputfile + "/." + filename + ".crc"

    newData.write
      .format("csv")
      .option("header", "true")
      .mode("overwrite")
      .save(outputFileName)
    merge(mergeFindGlob, mergedFileName,fileDel)
    newData.unpersist()

  }

  def merge(srcPath: String, dstPath: String,delPath: String): Unit =  {
    val hadoopConfig = new Configuration()
    val hdfs = FileSystem.get(hadoopConfig)
    copyMerge(hdfs, new Path(srcPath), hdfs, new Path(dstPath), true, hadoopConfig)
    // the "true" setting deletes the source files once they are merged into the new output
    hdfs.delete(new Path(delPath),true)
  }


  def copyMerge(srcFS: FileSystem, srcDir: Path, dstFS: FileSystem, dstFile: Path, deleteSource: Boolean, conf: Configuration): Boolean = {

    if (dstFS.exists(dstFile)) {
      //throw new IOException(s"Target $dstFile already exists")
      /////////////////
      dstFS.delete(dstFile,true)
    }
    // Source path is expected to be a directory:
    if (srcFS.getFileStatus(srcDir).isDirectory) {

      val outputFile = dstFS.create(dstFile)
      try {
        srcFS
          .listStatus(srcDir)
          .sortBy(_.getPath.getName)
          .collect {
            case status if status.isFile =>
              val inputFile = srcFS.open(status.getPath)
              try { IOUtils.copyBytes(inputFile, outputFile, conf, false) }
              finally { inputFile.close() }
          }
      } finally { outputFile.close() }

      if (deleteSource) srcFS.delete(srcDir, true) else true
    }
    else false
  }
}