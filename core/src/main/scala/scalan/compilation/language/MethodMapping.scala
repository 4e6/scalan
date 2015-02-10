package scalan.compilation.language

import scala.collection.mutable
import scala.language.postfixOps

object LanguageId extends Enumeration {
  type LANGUAGE = Value
  val SCALA, CPP = Value
}

trait MethodMapping {

  import LanguageId._

  var currentLanguage = SCALA

  val backends: mutable.HashMap[LANGUAGE, LanguageConf#Backend[_]] = new mutable.HashMap()

  def methodReplaceConf: LanguageConf#Backend[_] = backends(currentLanguage)

  trait Implicit[T] {
    this: T =>
    implicit val v: T = this
  }

  trait LanguageConf extends Implicit[LanguageConf] {

    import scala.reflect.runtime.universe.typeOf
    import scala.reflect.runtime.universe.Type

    val tyInt = typeOf[Int]
    val tyArray = typeOf[Array[_]]

    val backend: Backend[_]

    trait Lib {
      libs += this
    }

    val libs: mutable.HashSet[Lib] = new mutable.HashSet()

    case class Pack(pack: String)(implicit val lib: Library) extends Implicit[Pack]

    case class Family(familyName: Symbol)(implicit val pack: Pack = null) extends Implicit[Family]

    trait Ty {
      def name: Symbol
    }

    case class BaseTy(name: Symbol) extends Ty

    case class TyArg(name: Symbol)

    trait DomainType extends Ty

    case class ClassType(name: Symbol, synonym: Symbol, tyArgs: TyArg*)(implicit val family: Family = null, val pack: Pack = null) extends DomainType with Implicit[ClassType]

    case class MethodArg(name: String)

    case class Method(name: Symbol, args: List[MethodArg], tyRes: Type)(implicit val theType: ClassType)

    trait Fun {
      val funcName: Symbol
      val lib: Lib = null
    }

    abstract class Backend[TC <: LanguageConf](language: LANGUAGE)(implicit val l: TC) {
      backends += language -> this
      type Func <: Fun

      def get(classPath: String, method: String): Option[Func] = {
        methodMap.getOrElse((classPath, method), None)
      }

      lazy val libs = l.libs

      val libPaths: Set[String]

      val functionMap: Map[Method, Func]
      lazy val methodMap: Map[(String, String), Option[Func]] = functionMap.map { case (m, f) =>
        (((m.theType.family match {
          case f: Family => f.pack.pack + "." + f.familyName.name + "$"
          case _ =>
            m.theType.pack match {
              case p: Pack => p.pack + "."
              case _ => ""
            }
        }) + m.theType.name.name, m.name.name), Some(f))
      } toMap
    }

  }

  trait CppLanguage extends LanguageConf {

    case class CppLib(hfile: String, libfile: String) extends Lib with Implicit[CppLib]

    case class CppType(name: Symbol)

    case class CppArg(ty: CppType, name: Symbol)

    case class CppFunc(override val funcName: Symbol, args: CppArg*)(implicit override val lib: CppLib) extends Fun

    abstract class CppBackend extends Backend(CPP) {
      type Func = CppFunc

      lazy val libPaths: Set[String] = null /*not implemented*/
    }

  }

  trait ScalaLanguage extends LanguageConf {

    case class ScalaLib(val jar: String, pack: String) extends Lib with Implicit[ScalaLib]

    case class EmbeddedObject(name: String) extends Lib with Implicit[EmbeddedObject]

    case class ScalaType(name: Symbol)

    case class ScalaArg(ty: ScalaType, name: Symbol)

    case class ScalaFunc(funcName: Symbol, args: ScalaArg*)(implicit override val lib: Lib) extends Fun

    abstract class ScalaBackend extends Backend(SCALA) {
      type Func = ScalaFunc

      lazy val libPaths: Set[String] = libs filter(_.isInstanceOf[ScalaLib]) map (_.asInstanceOf[ScalaLib].jar) filter (!_.isEmpty) to
    }

  }

}

