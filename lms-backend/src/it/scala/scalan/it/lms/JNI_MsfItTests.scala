package scalan.it.lms

import java.io.File

import scala.language.reflectiveCalls
import scalan._
import scalan.compilation.lms.JNIBridge
import scalan.compilation.lms.cxx.LmsCompilerCxx
import scalan.compilation.lms.cxx.sharedptr.CoreCxxShptrLmsBackend
import scalan.compilation.{GraphVizConfig, GraphVizExport}
import scalan.graphs.{GraphsDslSeq, GraphsDsl, GraphsDslExp}
import scalan.it.BaseCtxItTests
import scalan.linalgebra.{MatricesDslExp, VectorsDslExp}

trait JNIMsfProg extends JNIExtractorOps with MsfFuncs {
  lazy val MSF_JNI_adjlist = JNI_Wrap(msfFunAdjBase)

  lazy val MSF_JNI_adjmatrix = JNI_Wrap(msfFunIncBase)
}

class JNI_MsfItTests extends BaseCtxItTests[JNIMsfProg](new ScalanCommunityDslSeq with JNIExtractorOpsSeq with GraphsDslSeq with JNIMsfProg) {

  class ProgExp extends ScalanCommunityDslExp with JNIExtractorOpsExp with GraphsDslExp with JNIMsfProg

  val compiler = new LmsCompilerCxx(new ProgExp) with JNIBridge

  class Ctx extends TestCompilerContext("MSF_JNI-cxx") {
    val compiler = JNI_MsfItTests.this.compiler
  }

  lazy val defaultCompilers = compilers(compiler)

  test("MSF_JNI") {
    val ctx1 = new Ctx

    val ctx2 = new Ctx

    ctx1.test("MSF_JNI_adjlist", ctx1.compiler.scalan.MSF_JNI_adjlist)
    ctx2.test("MSF_JNI_adjmatrix", ctx2.compiler.scalan.MSF_JNI_adjmatrix)
  }
}
