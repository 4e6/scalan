/**
 * User: Alexander Slesarenko
 * Date: 12/15/13
 */
package scalan.meta

import java.io.File
import com.typesafe.scalalogging.slf4j.LazyLogging
import scalan.util.FileUtil
import ScalanAst._

class EntityManagement(val config: CodegenConfig) extends ScalanParsersEx with LazyLogging {

  case class EntityManager(name: String, file: File, entityDef: SEntityModuleDef, config: CodegenConfig)

  private val entities = config.entityFiles.flatMap { f =>
    val file = FileUtil.file(config.srcPath, f)
    try {
      val d = parseEntityModule(file)
      Some(new EntityManager(d.name, file, d, config))
    } catch {
      case e: Exception =>
        logger.error(s"Failed to parse file at $file (relative to ${FileUtil.currentWorkingDir})", e)
        None
    }
  }

  def generateAll() = {
    entities.foreach { m =>
      println(s"  generating ${m.file}")
      val g = new ScalanCodegen.EntityFileGenerator(m.entityDef, m.config)
      val implCode = g.getImplFile
      saveEntity(m.file, implCode)
    }
  }

  def saveEntity(file: File, implCode: String) = {
    val fileName = file.getName.split('.')(0)
    val folder = file.getParentFile
    val implFile = FileUtil.file(folder, "impl", s"${fileName}Impl.scala")

    implFile.mkdirs()

    FileUtil.write(implFile, implCode)
  }

}
