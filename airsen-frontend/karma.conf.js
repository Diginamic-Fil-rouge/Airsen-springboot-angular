// Karma configuration file for Airsen Frontend
// See http://karma-runner.github.io/latest/config/configuration-file.html

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    client: {
      jasmine: {
        // Jasmine configuration
        random: false, // Run tests in order for consistency
        seed: 42, // Seed for random test order (if random: true)
        stopSpecOnExpectationFailure: false, // Continue running tests after failure
        stopOnSpecFailure: false, // Continue running tests after spec failure
        timeoutInterval: 10000 // Default timeout for async tests (10 seconds)
      },
      clearContext: false // leave Jasmine Spec Runner output visible in browser
    },
    jasmineHtmlReporter: {
      suppressAll: false, // Show all messages
      suppressFailed: false // Show failed messages
    },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/airsen-frontend'),
      subdir: '.',
      reporters: [
        { type: 'html' }, // HTML report for viewing in browser
        { type: 'text' }, // Text summary in console
        { type: 'text-summary' }, // Brief summary in console
        { type: 'lcovonly' } // LCOV format for CI tools
      ],
      check: {
        global: {
          statements: 50,
          branches: 40,
          functions: 40,
          lines: 50
        }
      }
    },
    reporters: ['progress', 'kjhtml', 'coverage'],
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: true,
    browsers: ['Chrome'],
    customLaunchers: {
      ChromeHeadlessCI: {
        base: 'ChromeHeadless',
        flags: [
          '--no-sandbox',
          '--disable-gpu',
          '--disable-dev-shm-usage',
          '--disable-software-rasterizer',
          '--disable-extensions'
        ]
      },
      ChromeDebug: {
        base: 'Chrome',
        flags: [
          '--remote-debugging-port=9333',
          '--disable-background-timer-throttling',
          '--disable-renderer-backgrounding',
          '--disable-backgrounding-occluded-windows'
        ]
      }
    },
    singleRun: false,
    restartOnFileChange: true,
    browserDisconnectTimeout: 10000,
    browserDisconnectTolerance: 3,
    browserNoActivityTimeout: 60000
  });
};
