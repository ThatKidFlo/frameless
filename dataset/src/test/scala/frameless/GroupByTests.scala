package frameless

import frameless.functions._
import frameless.functions.aggregate._
import org.scalacheck.Prop
import org.scalacheck.Prop._

class GroupByTests extends TypedDatasetSuite {
  // Datasets are coalesced due to https://issues.apache.org/jira/browse/SPARK-12675

  test("groupBy('a).agg(sum('b))") {
    def prop[
      A: TypedEncoder : Ordering,
      B: TypedEncoder : Summable : Numeric
    ](data: List[X2[A, B]]): Prop = {
      val dataset = TypedDataset.create(data).coalesce(2)
      val A = dataset.col[A]('a)
      val B = dataset.col[B]('b)

      val datasetSumByA = dataset.groupBy(A).agg(sum(B)).collect().run.toVector.sortBy(_._1)
      val sumByA = data.groupBy(_.a).mapValues(_.map(_.b).sum).toVector.sortBy(_._1)

      datasetSumByA ?= sumByA
    }

    check(forAll(prop[Int, Long] _))
  }

  test("groupBy('a).agg(sum('b), sum('c))") {
    def prop[
      A: TypedEncoder : Ordering,
      B: TypedEncoder : Summable : Numeric,
      C: TypedEncoder : Summable : Numeric
    ](data: List[X3[A, B, C]]): Prop = {
      val dataset = TypedDataset.create(data).coalesce(2)
      val A = dataset.col[A]('a)
      val B = dataset.col[B]('b)
      val C = dataset.col[C]('c)

      val datasetSumByAB = dataset
        .groupBy(A)
        .agg(sum(B), sum(C))
        .collect().run.toVector.sortBy(_._1)

      val sumByAB = data.groupBy(_.a).mapValues { xs =>
        (xs.map(_.b).sum, xs.map(_.c).sum)
      }.toVector.map {
        case (a, (b, c)) => (a, b, c)
      }.sortBy(_._1)

      datasetSumByAB ?= sumByAB
    }

    check(forAll(prop[String, Long, BigDecimal] _))
  }

  test("groupBy('a, 'b).agg(sum('c), sum('d))") {
    def prop[
      A: TypedEncoder,
      B: TypedEncoder,
      C: TypedEncoder : Summable : Numeric,
      D: TypedEncoder : Summable : Numeric
    ](data: List[X4[A, B, C, D]])(
      implicit o: Ordering[(A, B)] // to compare ordered vectors
    ): Prop = {
      val dataset = TypedDataset.create(data).coalesce(2)
      val A = dataset.col[A]('a)
      val B = dataset.col[B]('b)
      val C = dataset.col[C]('c)
      val D = dataset.col[D]('d)

      val datasetSumByAB = dataset
        .groupBy(A, B)
        .agg(sum(C), sum(D))
        .collect().run.toVector.sortBy(x => (x._1, x._2))

      val sumByAB = data.groupBy(x => (x.a, x.b)).mapValues { xs =>
        (xs.map(_.c).sum, xs.map(_.d).sum)
      }.toVector.map {
        case ((a, b), (c, d)) => (a, b, c, d)
      }.sortBy(x => (x._1, x._2))

      datasetSumByAB ?= sumByAB
    }

    check(forAll(prop[Byte, Int, Long, BigDecimal] _))
  }
}
