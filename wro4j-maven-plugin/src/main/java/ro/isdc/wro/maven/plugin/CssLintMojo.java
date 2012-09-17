/**
 * Copyright wro4j@2011
 */
package ro.isdc.wro.maven.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.apache.maven.plugin.MojoExecutionException;

import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.extensions.processor.css.CssLintProcessor;
import ro.isdc.wro.extensions.processor.support.csslint.CssLintError;
import ro.isdc.wro.extensions.processor.support.csslint.CssLintException;
import ro.isdc.wro.extensions.support.lint.LintReport;
import ro.isdc.wro.extensions.support.lint.ResourceLintReport;
import ro.isdc.wro.extensions.support.lint.XmlReportFormatter;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.processor.ResourcePreProcessor;


/**
 * Maven plugin used to validate css code defined in wro model.
 * 
 * @goal csslint
 * @phase compile
 * @requiresDependencyResolution runtime
 * @author Alex Objelean
 * @since 1.3.8
 * @created 20 Jun 2011
 */
public class CssLintMojo
    extends AbstractSingleProcessorMojo {
  /**
   * File where the report will be written.
   * 
   * @parameter default-value="${project.build.directory}/wro4j-reports/csslint.xml" expression="${reportFile}"
   * @optional
   */
  private File reportFile;
  /**
   * Contains errors found during jshint processing which will be reported eventually.
   */
  private LintReport<CssLintError> lintReport;
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected ResourcePreProcessor createResourceProcessor() {
    final ResourcePreProcessor processor = new CssLintProcessor() {
      @Override
      public void process(final Resource resource, final Reader reader, final Writer writer)
          throws IOException {
        if (resource != null) {
          getLog().info("processing resource: " + resource.getUri());
        }
        super.process(resource, reader, writer);
      }
      
      @Override
      protected void onException(final WroRuntimeException e) {
        CssLintMojo.this.onException(e);
      }
      
      @Override
      protected void onCssLintException(final CssLintException e, final Resource resource)
          throws Exception {
        getLog().error(
            e.getErrors().size() + " errors found while processing resource: " + resource.getUri() + " Errors are: "
                + e.getErrors());
        // collect found errors
        lintReport.addReport(ResourceLintReport.create(resource.getUri(), e.getErrors()));
        if (!isFailNever()) {
          throw new MojoExecutionException("Errors found when validating resource: " + resource);
        }
      };
    }.setOptions(getOptions());
    return processor;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected void onBeforeExecute() {
    lintReport = new LintReport<CssLintError>();
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected void onAfterExecute() {
    if (reportFile != null) {
      try {
        getLog().debug("creating report at location: " + reportFile);
        XmlReportFormatter.createForCssLintError(lintReport, XmlReportFormatter.Type.CSSLINT).write(
            new FileOutputStream(reportFile));
      } catch (FileNotFoundException e) {
        getLog().error("Could not create report file: " + reportFile, e);
      }
    }
  }
  
  /**
   * @VisibleForTesting
   */
  void setReportFile(final File reportFile) {
    this.reportFile = reportFile;
  }
  
  /**
   * Used by unit test to check if mojo doesn't fail.
   */
  void onException(final Exception e) {
  }
}
