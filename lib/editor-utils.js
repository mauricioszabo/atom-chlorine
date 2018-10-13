var CompositeDisposable, EditorUtils, Point, Range, edn_reader;

({CompositeDisposable, Range, Point} = require('atom'));

edn_reader = require('./edn_reader.js');

module.exports = EditorUtils = {
  // Escapes the Clojure code and places it in quoatations
  escapeClojureCodeInString: function(code) {
    var escaped;
    escaped = code.replace(/\\/g, "\\\\").replace(/"/g, "\\\"");
    return `"${escaped}"`;
  },
  // Finds a Clojure Namespace declaration in the editor and returns the name
  // of the namespace.
  findNsDeclaration: function(editor) {
    var e, i, len, range, ref, txt;
    ref = this.getTopLevelRanges(editor);
    for (i = 0, len = ref.length; i < len; i++) {
      range = ref[i];
      txt = editor.getTextInBufferRange(range);
      try {
        if (txt.match(/^\(ns /)) {
          return edn_reader.parse(txt)[1];
        }
      } catch (error) {
        e = error;
        return null;
      }
    }
  },
  // Returns true if the position in the text editor is in a Markdown file in a
  // code block that contains Clojure.
  isPosInClojureMarkdown: function(editor, pos) {
    var scopeDesc;
    scopeDesc = editor.scopeDescriptorForBufferPosition(pos);
    return scopeDesc.scopes.indexOf("markup.code.clojure.gfm") >= 0;
  },
  // Finds a starting markdown section like "```clojure" searching backwards
  // from fromPos.
  findMarkdownCodeBlockStartPosition: function(editor, fromPos) {
    var scanRange, startPos;
    startPos = null;
    // We translate the search range forward in case the cursor is in the middle
    // of the declaration of the markdown block.
    scanRange = new Range([0, 0], fromPos.translate(new Point(0, 10)));
    editor.backwardsScanInBufferRange(/```clojure/ig, scanRange, function(result) {
      startPos = result.range.start.translate(new Point(1, 0));
      return result.stop();
    });
    return startPos;
  },
  // Finds a closing markdown section "```" searching forwards from fromPos.
  findMarkdownCodeBlockEndPosition: function(editor, fromPos) {
    var endPos, scanRange;
    endPos = null;
    scanRange = new Range(fromPos, editor.buffer.getEndPosition());
    editor.scanInBufferRange(/```/g, scanRange, function(result) {
      endPos = result.range.start;
      return result.stop();
    });
    return endPos;
  },
  // Takes an editor and the position of a brace found in scanning and Determines
  // if the brace found at that position can be ignored. If the brace is in a
  // comment or inside a string it can be ignored.
  isIgnorableBrace: function(editor, pos) {
    var scopes;
    scopes = editor.scopeDescriptorForBufferPosition(pos).scopes;
    return scopes.indexOf("string.quoted.double.clojure") >= 0 || scopes.indexOf("comment.line.semicolon.clojure") >= 0 || scopes.indexOf("string.regexp.clojure") >= 0;
  },
  findBlockStartPosition: function(editor, fromPos) {
    var braceClosed, openToClose, startPos;
    braceClosed = {
      "}": 0,
      ")": 0,
      "]": 0
    };
    openToClose = {
      "{": "}",
      "[": "]",
      "(": ")"
    };
    startPos = null;
    editor.backwardsScanInBufferRange(/[\{\}\[\]\(\)]/g, new Range([0, 0], fromPos), (result) => {
      var c;
      if (!(this.isIgnorableBrace(editor, result.range.start))) {
        c = "" + result.match[0];
        if (braceClosed[c] !== void 0) {
          return braceClosed[c]++;
        } else {
          braceClosed[openToClose[c]]--;
          if (braceClosed[openToClose[c]] === -1) {
            startPos = result.range.start;
            return result.stop();
          }
        }
      }
    });
    return startPos;
  },
  findBlockEndPosition: function(editor, fromPos) {
    var braceOpened, closeToOpen, endPos, scanRange;
    braceOpened = {
      "{": 0,
      "(": 0,
      "[": 0
    };
    closeToOpen = {
      "}": "{",
      "]": "[",
      ")": "("
    };
    endPos = null;
    scanRange = new Range(fromPos, editor.buffer.getEndPosition());
    editor.scanInBufferRange(/[\{\}\[\]\(\)]/g, scanRange, (result) => {
      var c;
      if (!(this.isIgnorableBrace(editor, result.range.start))) {
        c = "" + result.match[0];
        if (braceOpened[c] !== void 0) {
          return braceOpened[c]++;
        } else {
          braceOpened[closeToOpen[c]]--;
          if (braceOpened[closeToOpen[c]] === -1) {
            endPos = result.range.start;
            return result.stop();
          }
        }
      }
    });
    return endPos;
  },
  // Determines if the cursor is directly after a closed block. If it is returns
  // the text of that block
  directlyAfterBlockRange: function(editor) {
    var pos, previousChar, previousPos, startPos;
    pos = editor.getCursorBufferPosition();
    if (pos.column > 0) {
      previousPos = new Point(pos.row, pos.column - 1);
      previousChar = editor.getTextInBufferRange(new Range(previousPos, pos));
      if ([")", "}", "]"].indexOf(previousChar) >= 0) {
        if (startPos = this.findBlockStartPosition(editor, previousPos)) {
          return new Range(startPos, pos);
        }
      }
    }
  },
  // Determines if the cursor is directly before a block opening. If it is returns
  // the text of that block
  directlyBeforeBlockRange: function(editor) {
    var afterChar, closingPos, endPos, pos, subsequentPos;
    pos = editor.getCursorBufferPosition();
    subsequentPos = pos.translate(new Point(0, 1));
    afterChar = editor.getTextInBufferRange(new Range(pos, subsequentPos));
    if (["(", "{", "["].indexOf(afterChar) >= 0) {
      if (endPos = this.findBlockEndPosition(editor, subsequentPos)) {
        closingPos = endPos.translate(new Point(0, 1));
        return new Range(pos, closingPos);
      }
    }
  },
  getCursorInClojureBlockRange: function(editor) {
    var closingPos, endPos, pos, range, startPos;
    if (range = this.directlyAfterBlockRange(editor)) {
      return range;
    } else if (range = this.directlyBeforeBlockRange(editor)) {
      return range;
    } else {
      pos = editor.getCursorBufferPosition();
      startPos = this.findBlockStartPosition(editor, pos);
      endPos = this.findBlockEndPosition(editor, pos);
      if (startPos && endPos) {
        closingPos = endPos.translate(new Point(0, 1));
        // Note that this will if used instead of executing the code implement expand selection on
        // repeated executions
        //editor.setSelectedBufferRange(new Range(startPos, closingPos))
        return new Range(startPos, closingPos);
      }
    }
  },
  getCursorInMarkdownBlockRange: function(editor) {
    var endPos, pos, startPos;
    pos = editor.getCursorBufferPosition();
    if (this.isPosInClojureMarkdown(editor, pos)) {
      if (startPos = this.findMarkdownCodeBlockStartPosition(editor, pos)) {
        // It's safer to search from the start of the startPos instead of searching from the end position
        if (endPos = this.findMarkdownCodeBlockEndPosition(editor, startPos)) {
          return new Range(startPos, endPos);
        }
      }
    }
  },
  // If the cursor is located in a Clojure block (in parentheses, brackets, or
  // braces) or next to one returns the text of that block. Also works with
  // Markdown blocks of code starting with  ```clojure  and ending with ```.
  getCursorInBlockRange: function(editor, {topLevel} = {
      topLevel: false
    }) {
    var range;
    if (topLevel && (range = this.getCursorInClojureTopBlockRange(editor, {
      lookInComments: true
    }))) {
      return range;
    } else if (range = this.getCursorInClojureBlockRange(editor)) {
      return range;
    } else {
      return this.getCursorInMarkdownBlockRange(editor);
    }
  },
  // Constructs a list of `Range`s, one for each top level form.
  getTopLevelRanges: function(editor, {lookInComments} = {
      lookInComments: false
    }) {
    var braceOpened, inTopLevelComment, ranges, rex;
    ranges = [];
    braceOpened = 0;
    inTopLevelComment = false;
    if (lookInComments) {
      rex = /(\(comment\s|[\{\}\[\]\(\)])/g;
    } else {
      rex = /[\{\}\[\]\(\)]/g;
    }
    editor.scan(rex, (result) => {
      var c, matchesComment;
      if (!(this.isIgnorableBrace(editor, result.range.start))) {
        matchesComment = result.matchText.match(/^\(comment\s/);
        if (matchesComment && braceOpened === 0) {
          inTopLevelComment = true;
        }
        c = "" + result.match[0];
        if (["(", "{", "["].indexOf(c) >= 0 || matchesComment) {
          if ((braceOpened === 1 && inTopLevelComment === true) || (braceOpened === 0 && inTopLevelComment === false)) {
            ranges.push([result.range.start]);
          }
          return braceOpened++;
        } else if ([")", "}", "]"].indexOf(c) >= 0) {
          braceOpened--;
          if ((braceOpened === 1 && inTopLevelComment === true) || (braceOpened === 0 && inTopLevelComment === false)) {
            ranges[ranges.length - 1].push(result.range.end);
          }
          if (braceOpened === 0 && inTopLevelComment === true) {
            return inTopLevelComment = false;
          }
        }
      }
    });
    return ranges.filter(function(range) {
      return range.length === 2;
    }).map(function(range) {
      return Range.fromObject(range);
    });
  },
  // Returns the `Range` that corresponds to the top level form that contains the current cursor position.
  // Doesn't work in Markdown blocks of code.
  getCursorInClojureTopBlockRange: function(editor, options = {}) {
    var pos, topLevelRanges;
    pos = editor.getCursorBufferPosition();
    topLevelRanges = this.getTopLevelRanges(editor, options);
    return topLevelRanges.find(function(range) {
      return range.containsPoint(pos);
    });
  },
  // Searches all open text editors for the given string. Returns a tuple of the
  // editor found and the range within the editor for the first location that
  // matches
  findEditorRangeContainingString: function(str) {
    var editor, editors, foundRange, i, len, regex;
    editors = atom.workspace.getTextEditors();
    // Create a literal regex to search for the given str
    // From http://stackoverflow.com/questions/3561493/is-there-a-regexp-escape-function-in-javascript
    regex = new RegExp(str.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&'));
    for (i = 0, len = editors.length; i < len; i++) {
      editor = editors[i];
      foundRange = null;
      editor.scan(regex, (matched) => {
        foundRange = matched.range;
        return matched.stop();
      });
      if (foundRange) {
        return [editor, foundRange];
      }
    }
  },
  // Determines if a cursor is within a range of text of a var and returns the text
  getClojureVarUnderCursor: function(editor) {
    return editor.getWordUnderCursor({
      wordRegex: /[a-zA-Z0-9\-.$!?\/><*=_]+/
    });
  }
};
