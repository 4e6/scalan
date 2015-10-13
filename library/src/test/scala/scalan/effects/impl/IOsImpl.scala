package scalan.examples

import scala.reflect.runtime.universe._
import scalan._
import scalan.monads._
import scala.reflect.runtime.universe.{WeakTypeTag, weakTypeTag}
import scalan.meta.ScalanAst._

package impl {
// Abs -----------------------------------
trait IOsAbs extends IOs with scalan.Scalan {
  self: IOsDsl =>

  // single proxy for each type family
  implicit def proxyIO[A](p: Rep[IO[A]]): IO[A] = {
    proxyOps[IO[A]](p)(scala.reflect.classTag[IO[A]])
  }

  // familyElem
  class IOElem[A, To <: IO[A]](implicit _eA: Elem[A])
    extends EntityElem[To] {
    def eA = _eA
    lazy val parent: Option[Elem[_]] = None
    lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map("A" -> Left(eA))
    }
    override def isEntityType = true
    override lazy val tag = {
      implicit val tagA = eA.tag
      weakTypeTag[IO[A]].asInstanceOf[WeakTypeTag[To]]
    }
    override def convert(x: Rep[Def[_]]) = {
      implicit val eTo: Elem[To] = this
      val conv = fun {x: Rep[IO[A]] => convertIO(x) }
      tryConvert(element[IO[A]], this, x, conv)
    }

    def convertIO(x: Rep[IO[A]]): Rep[To] = {
      x.selfType1 match {
        case _: IOElem[_, _] => x.asRep[To]
        case e => !!!(s"Expected $x to have IOElem[_, _], but got $e")
      }
    }

    override def getDefaultRep: Rep[To] = ???
  }

  implicit def iOElement[A](implicit eA: Elem[A]): Elem[IO[A]] =
    cachedElem[IOElem[A, IO[A]]](eA)

  implicit case object IOCompanionElem extends CompanionElem[IOCompanionAbs] {
    lazy val tag = weakTypeTag[IOCompanionAbs]
    protected def getDefaultRep = IO
  }

  abstract class IOCompanionAbs extends CompanionDef[IOCompanionAbs] with IOCompanion {
    def selfType = IOCompanionElem
    override def toString = "IO"
  }
  def IO: Rep[IOCompanionAbs]
  implicit def proxyIOCompanionAbs(p: Rep[IOCompanionAbs]): IOCompanionAbs =
    proxyOps[IOCompanionAbs](p)

  abstract class AbsReadFile
      (fileName: Rep[String])
    extends ReadFile(fileName) with Def[ReadFile] {
    lazy val selfType = element[ReadFile]
  }
  // elem for concrete class
  class ReadFileElem(val iso: Iso[ReadFileData, ReadFile])
    extends IOElem[List[String], ReadFile]
    with ConcreteElem[ReadFileData, ReadFile] {
    override lazy val parent: Option[Elem[_]] = Some(iOElement(listElement(StringElement)))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map()
    }

    override def convertIO(x: Rep[IO[List[String]]]) = // Converter is not generated by meta
!!!("Cannot convert from IO to ReadFile: missing fields List(fileName)")
    override def getDefaultRep = ReadFile("")
    override lazy val tag = {
      weakTypeTag[ReadFile]
    }
  }

  // state representation type
  type ReadFileData = String

  // 3) Iso for concrete class
  class ReadFileIso
    extends Iso[ReadFileData, ReadFile] {
    override def from(p: Rep[ReadFile]) =
      p.fileName
    override def to(p: Rep[String]) = {
      val fileName = p
      ReadFile(fileName)
    }
    lazy val eTo = new ReadFileElem(this)
  }
  // 4) constructor and deconstructor
  class ReadFileCompanionAbs extends CompanionDef[ReadFileCompanionAbs] with ReadFileCompanion {
    def selfType = ReadFileCompanionElem
    override def toString = "ReadFile"

    def apply(fileName: Rep[String]): Rep[ReadFile] =
      mkReadFile(fileName)
  }
  object ReadFileMatcher {
    def unapply(p: Rep[IO[List[String]]]) = unmkReadFile(p)
  }
  lazy val ReadFile: Rep[ReadFileCompanionAbs] = new ReadFileCompanionAbs
  implicit def proxyReadFileCompanion(p: Rep[ReadFileCompanionAbs]): ReadFileCompanionAbs = {
    proxyOps[ReadFileCompanionAbs](p)
  }

  implicit case object ReadFileCompanionElem extends CompanionElem[ReadFileCompanionAbs] {
    lazy val tag = weakTypeTag[ReadFileCompanionAbs]
    protected def getDefaultRep = ReadFile
  }

  implicit def proxyReadFile(p: Rep[ReadFile]): ReadFile =
    proxyOps[ReadFile](p)

  implicit class ExtendedReadFile(p: Rep[ReadFile]) {
    def toData: Rep[ReadFileData] = isoReadFile.from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoReadFile: Iso[ReadFileData, ReadFile] =
    cachedIso[ReadFileIso]()

  // 6) smart constructor and deconstructor
  def mkReadFile(fileName: Rep[String]): Rep[ReadFile]
  def unmkReadFile(p: Rep[IO[List[String]]]): Option[(Rep[String])]

  abstract class AbsWriteFile
      (fileName: Rep[String], lines: Rep[List[String]])
    extends WriteFile(fileName, lines) with Def[WriteFile] {
    lazy val selfType = element[WriteFile]
  }
  // elem for concrete class
  class WriteFileElem(val iso: Iso[WriteFileData, WriteFile])
    extends IOElem[Unit, WriteFile]
    with ConcreteElem[WriteFileData, WriteFile] {
    override lazy val parent: Option[Elem[_]] = Some(iOElement(UnitElement))
    override lazy val tyArgSubst: Map[String, TypeDesc] = {
      Map()
    }

    override def convertIO(x: Rep[IO[Unit]]) = // Converter is not generated by meta
!!!("Cannot convert from IO to WriteFile: missing fields List(fileName, lines)")
    override def getDefaultRep = WriteFile("", element[List[String]].defaultRepValue)
    override lazy val tag = {
      weakTypeTag[WriteFile]
    }
  }

  // state representation type
  type WriteFileData = (String, List[String])

  // 3) Iso for concrete class
  class WriteFileIso
    extends Iso[WriteFileData, WriteFile]()(pairElement(implicitly[Elem[String]], implicitly[Elem[List[String]]])) {
    override def from(p: Rep[WriteFile]) =
      (p.fileName, p.lines)
    override def to(p: Rep[(String, List[String])]) = {
      val Pair(fileName, lines) = p
      WriteFile(fileName, lines)
    }
    lazy val eTo = new WriteFileElem(this)
  }
  // 4) constructor and deconstructor
  class WriteFileCompanionAbs extends CompanionDef[WriteFileCompanionAbs] with WriteFileCompanion {
    def selfType = WriteFileCompanionElem
    override def toString = "WriteFile"
    def apply(p: Rep[WriteFileData]): Rep[WriteFile] =
      isoWriteFile.to(p)
    def apply(fileName: Rep[String], lines: Rep[List[String]]): Rep[WriteFile] =
      mkWriteFile(fileName, lines)
  }
  object WriteFileMatcher {
    def unapply(p: Rep[IO[Unit]]) = unmkWriteFile(p)
  }
  lazy val WriteFile: Rep[WriteFileCompanionAbs] = new WriteFileCompanionAbs
  implicit def proxyWriteFileCompanion(p: Rep[WriteFileCompanionAbs]): WriteFileCompanionAbs = {
    proxyOps[WriteFileCompanionAbs](p)
  }

  implicit case object WriteFileCompanionElem extends CompanionElem[WriteFileCompanionAbs] {
    lazy val tag = weakTypeTag[WriteFileCompanionAbs]
    protected def getDefaultRep = WriteFile
  }

  implicit def proxyWriteFile(p: Rep[WriteFile]): WriteFile =
    proxyOps[WriteFile](p)

  implicit class ExtendedWriteFile(p: Rep[WriteFile]) {
    def toData: Rep[WriteFileData] = isoWriteFile.from(p)
  }

  // 5) implicit resolution of Iso
  implicit def isoWriteFile: Iso[WriteFileData, WriteFile] =
    cachedIso[WriteFileIso]()

  // 6) smart constructor and deconstructor
  def mkWriteFile(fileName: Rep[String], lines: Rep[List[String]]): Rep[WriteFile]
  def unmkWriteFile(p: Rep[IO[Unit]]): Option[(Rep[String], Rep[List[String]])]

  registerModule(IOs_Module)
}

// Seq -----------------------------------
trait IOsSeq extends IOsDsl with scalan.ScalanSeq {
  self: IOsDslSeq =>
  lazy val IO: Rep[IOCompanionAbs] = new IOCompanionAbs {
  }

  case class SeqReadFile
      (override val fileName: Rep[String])
    extends AbsReadFile(fileName) {
  }

  def mkReadFile
    (fileName: Rep[String]): Rep[ReadFile] =
    new SeqReadFile(fileName)
  def unmkReadFile(p: Rep[IO[List[String]]]) = p match {
    case p: ReadFile @unchecked =>
      Some((p.fileName))
    case _ => None
  }

  case class SeqWriteFile
      (override val fileName: Rep[String], override val lines: Rep[List[String]])
    extends AbsWriteFile(fileName, lines) {
  }

  def mkWriteFile
    (fileName: Rep[String], lines: Rep[List[String]]): Rep[WriteFile] =
    new SeqWriteFile(fileName, lines)
  def unmkWriteFile(p: Rep[IO[Unit]]) = p match {
    case p: WriteFile @unchecked =>
      Some((p.fileName, p.lines))
    case _ => None
  }
}

// Exp -----------------------------------
trait IOsExp extends IOsDsl with scalan.ScalanExp {
  self: IOsDslExp =>
  lazy val IO: Rep[IOCompanionAbs] = new IOCompanionAbs {
  }

  case class ExpReadFile
      (override val fileName: Rep[String])
    extends AbsReadFile(fileName)

  object ReadFileMethods {
    object toOper {
      def unapply(d: Def[_]): Option[Rep[ReadFile]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[ReadFileElem] && method.getName == "toOper" =>
          Some(receiver).asInstanceOf[Option[Rep[ReadFile]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[ReadFile]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object ReadFileCompanionMethods {
  }

  def mkReadFile
    (fileName: Rep[String]): Rep[ReadFile] =
    new ExpReadFile(fileName)
  def unmkReadFile(p: Rep[IO[List[String]]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: ReadFileElem @unchecked =>
      Some((p.asRep[ReadFile].fileName))
    case _ =>
      None
  }

  case class ExpWriteFile
      (override val fileName: Rep[String], override val lines: Rep[List[String]])
    extends AbsWriteFile(fileName, lines)

  object WriteFileMethods {
    object toOper {
      def unapply(d: Def[_]): Option[Rep[WriteFile]] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[WriteFileElem] && method.getName == "toOper" =>
          Some(receiver).asInstanceOf[Option[Rep[WriteFile]]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[WriteFile]] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object WriteFileCompanionMethods {
  }

  def mkWriteFile
    (fileName: Rep[String], lines: Rep[List[String]]): Rep[WriteFile] =
    new ExpWriteFile(fileName, lines)
  def unmkWriteFile(p: Rep[IO[Unit]]) = p.elem.asInstanceOf[Elem[_]] match {
    case _: WriteFileElem @unchecked =>
      Some((p.asRep[WriteFile].fileName, p.asRep[WriteFile].lines))
    case _ =>
      None
  }

  object IOMethods {
    object toOper {
      def unapply(d: Def[_]): Option[Rep[IO[A]] forSome {type A}] = d match {
        case MethodCall(receiver, method, _, _) if receiver.elem.isInstanceOf[IOElem[_, _]] && method.getName == "toOper" =>
          Some(receiver).asInstanceOf[Option[Rep[IO[A]] forSome {type A}]]
        case _ => None
      }
      def unapply(exp: Exp[_]): Option[Rep[IO[A]] forSome {type A}] = exp match {
        case Def(d) => unapply(d)
        case _ => None
      }
    }
  }

  object IOCompanionMethods {
  }
}

object IOs_Module extends scalan.ModuleInfo {
  val dump = "H4sIAAAAAAAAALVWPYwbRRSeXZ//T7nkgEMgRVyMyZEI7AgJpbgiulx86NDGPt0mgJwINF6PnQmzs3s749OaIgVFCugQLUUkyjSIBgkpDUJCFFQIIVFThURRClKBeDv747XjPUyBi9HO7Jv3873ve967D1BeeOi0sDDDvGETiRumet4Ssm62uKRyfNnpjxi5RAYfr31jXeYXhY5WuqhwA4tLgnVROXxo+W7ybJIDA5Uxt4iQjickOmWoCE3LYYxYkjq8SW17JHGPkaZBhdw00FLP6Y8P0C2kGei45XDLI5KY2wwLQUR0XiJBRjTZl9V+3HEnMXgzqKKZquKKh6mE9CHG8dB+n7jmmDt8bEt0LEqt4wZpgU2R2q7jyThEEdzdcPrxdoljOECrxk18iJsQYtg0pUf5EG5WXWx9iIekDSaB+RIkLAgbXBm7ap8zUEWQAwBo13aZOvFdhBB04A2VRGOCTyPBpxHgUzeJRzGjH+Hg5Z7n+GMU/rQcQr4LLl77FxexB9Li/fon161rT8yqrQeX/SCVoqqwAI5eymCDagXg+MP+Z+LxW3fO66jSRRUqtnpCetiS6ZZHaFUx545UOScAYm8I3apldUtF2QKbGUqULcd2MQdPEZTL0CdGLSoD4+BsOepOBvRF6ZLYVPNdLal3PaNexZttzNje/Rdef+WP1ns60qdDlMGlCcT3YqcS6budyHGwrkgwA+yTWC9nxXLJnkdt4PYhefO7b68+utfOq3CrfTLAIybfwWxEQqZFwSeJqLi1mkSFiUHZn12LR5SagL5x/2H/+3Poup60KqpsMXaAi7z49Zfqz2cu6KjUVVraYXjYhW6JFiN2x9t2uOyiknNIvPBN8RCz4GkuW4pR+VEP0+DnAHyJ1jNV75KgM5tKYVoMQDUUSdvhpL6zV//T/PHzu4EGPLQcvgnHwN/0/F+/HRtIJQ+JSgPKlKrjpuZggoSIBMvz86CuhP5MxyYnao/p+3c+lQpUzZ+eHp3eTZDrprp36gh84yn21e3bzz368oNnlPpKPSpt7NbP/QftxVL5H7WFFAiTqfLiZB8sNYD0xD7B/R2AdTsdujZ7B7CPDWfeV7VpnU0Lbw0OXj0D6rvKqXy6PSpAyvxkwhEVZdGOz7+bZ5QT8fTFORNhLnvSSW6o9ewCgK6+61FJFkG0nFhODFL1FKKQ00nngHpHAQ4H2lbKVQbmM3UsXl51tzOnrnDgqn0yoE5mj1ig6Nq+8Sx7cOGejvJvo/wA5o4wUL7njHg/5j58e0jiy4vxmTbNfeA69rCdcF391tEkqUwk2tN4g2EOMJRoJcqY+BhkF1FnA0qpZZRiRuqDntx68kX77E9f/67+LCqBjmG08eTrJf0nMd2tAoSGL5FUosDMQNYqyX8A07YtLBkKAAA="
}
}

