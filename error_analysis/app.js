var App = function () {
    var self = this;
    self.models = ko.observableArray([]);
    self.data = ko.observableArray([]);
    self.page = ko.observable(0);
    self.pageSize = 15;
    self.total = ko.observable(0);
    self.totalRels = ko.observable(0);
    self.TP = ko.observable(false);
    self.TN = ko.observable(false);
    self.FP = ko.observable(false);
    self.FN = ko.observable(true);
    self.CC = ko.observable(false);
    self.CI = ko.observable(false);
    self.IC = ko.observable(false);
    self.II = ko.observable(false);
    self.trMatched = ko.observable(false);
    self.lmMatched = ko.observable(false);
    self.spMatched = ko.observable(false);
    self.spTop10 = ko.observable(false);
    self.diffRole = ko.observable(false);
    self.diffRel = ko.observable(false);
    self.region = ko.observable(false);
    self.nonRegion = ko.observable(false);
    self.direction = ko.observable(false);
    self.nonDirection = ko.observable(false);
    self.general = ko.observable(false);
    self.nonGeneral = ko.observable(false);
    self.currentRel = ko.observable({ index: -1 });
    self.allRolesCorrect = ko.observable(false);
    self.comparison = ko.observable({
        first: {},
        second: {},
        cc: [],
        ci: [],
        ic: [],
        ii: []
    });

    self.spFilter = ko.observable("")

    self.addModel = function (m) {
        self.models.push(m);
    };

    self.removeModel = function (m) {
        self.models.remove(m);
    };

    self.setSelected = function (i, selected) {
        self.models()[i].selected = selected;
    };

    self.explore = function () {
        var selected = ko.utils.arrayFirst(self.models(), function (item) {
            return item.selected;
        });
        self.data([]);
        if (selected) {
            var d = selected.data.filter(function (item, i) {

                if (self.spFilter() !== "" && item.sp.toLowerCase() !== (self.spFilter().toLowerCase())) {
                    return false;
                }

                if (self.allRolesCorrect() && !(item.trApproved && item.spApproved && item.lmApproved))
                    return false;

                if (self.trMatched() && !item.trMatched)
                    return false;

                if (self.lmMatched() && !item.lmMatched)
                    return false;

                if (self.spMatched() && !item.spMatched)
                    return false;

                if (self.spTop10() && !item.spInTop10)
                    return false;

                if (self.general() && item.general == "None")
                    return false;

                if (self.nonGeneral() && item.general != "None")
                    return false;


                if (self.region() && item.region == "None")
                    return false;

                if (self.nonRegion() && item.region != "None")
                    return false;

                if (self.direction() && item.direction == "None")
                    return false;

                if (self.nonDirection() && item.direction != "None")
                    return false;

                if (self.comparison().first !== {} && self.comparison().second !== {}) {
                    if (self.diffRole() && self.models()[0].data[i].equalRoles(self.models()[1].data[i]))
                        return false;
                    if (self.diffRel() && self.models()[0].data[i].equalRels(self.models()[1].data[i]))
                        return false;

                    if (self.CC() && self.comparison().cc.indexOf(i) < 0)
                        return false;

                    if (self.CI() && self.comparison().ci.indexOf(i) < 0)
                        return false;

                    if (self.IC() && self.comparison().ic.indexOf(i) < 0)
                        return false;

                    if (self.II() && self.comparison().ii.indexOf(i) < 0)
                        return false;

                }

                if (self.TP() && item.tp)
                    return true;
                if (self.TN() && item.tn)
                    return true;
                if (self.FP() && item.fp)
                    return true;
                if (self.FN() && item.fn)
                    return true;
                return false;
            });

            self.totalRels(d.length);
            self.total(Math.ceil(d.length / self.pageSize));
            var from = self.page() * self.pageSize;
            var to = Math.min(d.length, (self.page() + 1) * self.pageSize);
            self.data(d.slice(from, to));
        }
    };


    self.toggleAllRolesCorrect = function () {
        self.allRolesCorrect(!self.allRolesCorrect());
        self.explore();
        return true;
    };

    self.toggleCC = function () {
        self.CC(!self.CC());
        self.explore();
        return true;
    };

    self.toggleCI = function () {
        self.CI(!self.CI());
        self.explore();
        return true;
    };

    self.toggleIC = function () {
        self.IC(!self.IC());
        self.explore();
        return true;
    };

    self.toggleII = function () {
        self.II(!self.II());
        self.explore();
        return true;
    };

    self.toggleTrMatched = function () {
        self.trMatched(!self.trMatched());
        self.explore();
        return true;
    };

    self.toggleLmMatched = function () {
        self.lmMatched(!self.lmMatched());
        self.explore();
        return true;
    };

    self.toggleSpMatched = function () {
        self.spMatched(!self.spMatched());
        self.explore();
        return true;
    };

    self.toggleSpTop10 = function () {
        self.spTop10(!self.spTop10());
        self.explore();
        return true;
    };

    self.toggleTP = function () {
        self.TP(!self.TP());
        self.explore();
        return true;
    };

    self.toggleTN = function () {
        self.TN(!self.TN());
        self.explore();
        return true;
    };

    self.toggleFP = function () {
        self.FP(!self.FP());
        self.explore();
        return true;
    };

    self.toggleFN = function () {
        self.FN(!self.FN());
        self.explore();
        return true;
    };

    self.toggleDiffRole = function () {
        self.diffRole(!self.diffRole());
        self.explore();
        return true;
    };

    self.toggleDiffRel = function () {
        self.diffRel(!self.diffRel());
        self.explore();
        return true;
    };


    self.toggleGeneral = function () {
        self.general(!self.general());
        self.explore();
        return true;
    };

    self.toggleNonGeneral = function () {
        self.nonGeneral(!self.nonGeneral());
        self.explore();
        return true;
    };

    self.toggleRegion = function () {
        self.region(!self.region());
        self.explore();
        return true;
    };

    self.toggleNonRegion = function () {
        self.nonRegion(!self.nonRegion());
        self.explore();
        return true;
    };

    self.toggleDirection = function () {
        self.direction(!self.direction());
        self.explore();
        return true;
    };

    self.toggleNonDirection = function () {
        self.nonDirection(!self.nonDirection());
        self.explore();
        return true;
    };

    self.nextPage = function () {
        self.page(Math.min(self.page() + 1, self.total() - 1));
        self.explore();
    };

    self.prevPage = function () {
        self.page(Math.max(self.page() - 1, 0));
        self.explore();
    };

    self.showRelDetail = function (m) {
        self.currentRel(m);
        self.explore();
    };

    self.compare = function () {
        var selected = self.models().filter(function (item) {
            return item.selected;
        });
        if (selected.length != 2) {
            alert("please select two models to compare!");
            return;
        }
        self.comparison(selected[0].compareWith(selected[1]));
    };

    self.trApproved = function (rel) {
        var str = "";
        for (i in self.models()) {
            str += self.models()[i].data[rel.index].trApproved ? '\u2713' : '\u00D7';
        }
        return str;
    }

    self.lmApproved = function (rel) {
        var str = "";
        for (i in self.models()) {
            str += self.models()[i].data[rel.index].lmApproved ? '\u2713' : '\u00D7';
        }
        return str;
    }

    self.spApproved = function (rel) {
        var str = "";
        for (i in self.models()) {
            str += self.models()[i].data[rel.index].spApproved ? '\u2713' : '\u00D7';
        }
        return str;
    }

    self.predicted = function (rel) {
        var str = "";
        for (i in self.models()) {
            str += self.models()[i].data[rel.index].tp || self.models()[i].data[rel.index].tn ? '\u2713' : '\u00D7';
        }
        return str;
    }
    self.generalApproved = function (rel) {
        var str = "";
        for (i in self.models()) {
            str += self.models()[i].data[rel.index].generalApproved ? '\u2713' : '\u00D7';
        }
        return str;
    }
    self.regionApproved = function (rel) {
        var str = "";
        for (i in self.models()) {
            str += self.models()[i].data[rel.index].regionApproved ? '\u2713' : '\u00D7';
        }
        return str;
    }
    self.directionApproved = function (rel) {
        var str = "";
        for (i in self.models()) {
            str += self.models()[i].data[rel.index].directionApproved ? '\u2713' : '\u00D7';
        }
        return str;
    }

    self.reset = function () {
        self.models([]);
        self.data([]);
        self.page(0);
        self.total(0);
        self.totalRels(0);
        self.TP(false);
        self.TN(false);
        self.FP(false);
        self.FN(true);
        self.CC(false);
        self.CI(false);
        self.IC(false);
        self.II(false);
        self.currentRel({ index: -1 });
        self.allRolesCorrect(false);
        self.comparison({
            first: {},
            second: {},
            cc: [],
            ci: [],
            ic: [],
            ii: []
        });
    }
};

var app = new App();
ko.applyBindings(app);
