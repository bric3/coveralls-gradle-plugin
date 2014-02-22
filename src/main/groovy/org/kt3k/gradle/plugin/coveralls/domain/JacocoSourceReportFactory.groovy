package org.kt3k.gradle.plugin.coveralls.domain

import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaPlugin


/**
 * Factory class of SourceReport for JaCoCo report file.
 */
class JacocoSourceReportFactory implements SourceReportFactory {

	@Override
	public List<SourceReport> createReportList(Project project, File jacocoReportFile) {
		List<File> targetSrcDirs = createTargetSrcDirs(project)
		return createReportList(targetSrcDirs.sort(), jacocoReportFile)
	}

	static List<File> createTargetSrcDirs(Project project) {
		List<File> targetSrcDirs = new ArrayList<File>()
		project.plugins.withType(JavaPlugin) {
			targetSrcDirs += project.sourceSets.main.java.srcDirs
		}
		project.plugins.withType(GroovyPlugin) {
			targetSrcDirs += project.sourceSets.main.groovy.srcDirs
		}
		project.plugins.withType(ScalaPlugin) {
			targetSrcDirs += project.sourceSets.main.scala.srcDirs
		}
		return targetSrcDirs.sort()
	}

	static List<SourceReport> createReportList(List<File> srcDirs, File jacocoReportFile) {
		XmlParser parser = new XmlParser()
		parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
		Node report = parser.parse(jacocoReportFile)
		Map<String, Map<Integer, Integer>> a = [:]
		report.package.each() { pkg ->
			pkg.sourcefile.each() { sf ->
				Map<Integer, Integer> cov = a.get("${pkg.@name}/${sf.@name}", [:])
				sf.line.each() { ln ->
					// jacoco doesn't count hits
					cov[ln.@nr.toInteger() - 1] = (ln.@ci.toInteger() > 0 ) ? 1 : 0;
				}
			}
		}

		List<SourceReport> reports = new ArrayList<SourceReport>()
		a.each { String filename, Map<Integer, Integer> cov ->
			File sourceFile = srcDirs.collect { new File(it, filename) }.find { it.exists() }
			if (sourceFile == null) {
				return;
			}
			String source = sourceFile.text
			List r = [null] * source.readLines().size()
			cov.each { Integer line, Integer hits ->
				r[line] = hits
			}
			reports.add new SourceReport(filename, source, r)
		}
		return reports
	}
}