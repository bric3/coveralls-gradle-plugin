package org.kt3k.gradle.plugin.coveralls.domain

class SourceReportFactory {

    public static List<SourceReport> createFromCoberturaXML(File file) {
        Node coverage = new XmlParser().parse(file)
        String sourceDir = coverage.sources.source.text() + '/'

        Map a = [:]

        coverage.packages.package.classes.class.each() { cls ->
            Map cov = a.get(cls.'@filename', [:])

            cls.lines.line.each() {
				Integer hits = cov.get(it.'@number'.toInteger() - 1, 0)
                cov[it.'@number'.toInteger() - 1] = hits + it.'@hits'.toInteger()
            }
        }

        List<SourceReport> reports = new ArrayList<SourceReport>()

        a.each { String filename, Map cov ->
            String source = new File(sourceDir + filename).text

            List r = [null] * source.readLines().size()
            cov.each { Integer line, Integer hits ->
                r[line] = hits
            }

            reports.add new SourceReport(filename, source, r)
        }

        return reports

    }
}
